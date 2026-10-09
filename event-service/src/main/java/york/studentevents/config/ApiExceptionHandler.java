package york.studentevents.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.ConflictException;
import york.studentevents.exceptions.EntityNotFoundException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.MissingVenueException;
import york.studentevents.exceptions.SubprocessException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.exceptions.VenueNotFoundException;

/**
 * Translates exceptions thrown anywhere in the controller and service layers into the standardised
 * {@link ApiErrorResponse} JSON format.
 *
 * <p>This is the only place exceptions are converted into HTTP responses, so controllers should
 * not catch these exceptions themselves. Spring selects the most specific matching handler for a
 * thrown exception, so the catch-all {@link #handleException} only applies to exceptions that no
 * other handler claims.
 *
 * <p>Stack traces are never included in a response; where a failure is a server-side fault they
 * are written to the log instead.
 *
 * @see ApiErrorResponse
 * @see ApiErrorCode
 */
@RestControllerAdvice
public class ApiExceptionHandler {
  
  // Used only for exceptions not specifically caught by the handler
  // (see ApiErrorCode.INTERNAL_ERROR)
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /**
   * Handles a request that referred to an entity that does not exist.
   *
   * <p>The error code is chosen from the concrete subclass of the exception.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 404 Not Found} response with the matching {@code *_NOT_FOUND} code
   * @throws IllegalStateException if the exception is a subclass that this handler does not know
   *     how to map to an error code
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException ex, HttpServletRequest request
  ) {

    ApiErrorCode errorCode;

    if (ex instanceof CohortNotFoundException) {
      errorCode = ApiErrorCode.COHORT_NOT_FOUND;
    } else if (ex instanceof EventNotFoundException) {
      errorCode = ApiErrorCode.EVENT_NOT_FOUND;
    } else if (ex instanceof UserNotFoundException) { 
      errorCode = ApiErrorCode.USER_NOT_FOUND;
    } else if (ex instanceof VenueNotFoundException) { 
      errorCode = ApiErrorCode.VENUE_NOT_FOUND;
    } else {
      /* This exception should never be thrown in production due to the
       * ApiEntityNotFoundExceptionHandlerCoverageTest preventing merges if a new exception is
       * implemented, but not included here.
       */
      throw new IllegalStateException("A subclass of EntityNotFoundException has been thrown, but"
          + " this subclass is not known to the ApiExceptionHandler.");
    }

    ApiErrorResponse response = new ApiErrorResponse(
        ex.getMessage(),
        errorCode,
        Instant.now(),
        request.getRequestURI()
    );

    return ResponseEntity.status(404).body(response);
  }

  /**
   * Handles a request rejected by the EventService when trying to assign event timings, despite the
   * event not yet having an assigned location.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 409 Conflict} response with the {@code MISSING_VENUE} code
   */
  @ExceptionHandler(MissingVenueException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingVenueException(
      MissingVenueException ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        ex.getMessage(),
        ApiErrorCode.MISSING_VENUE,
        Instant.now(),
        request.getRequestURI()
    );

    return ResponseEntity.status(409).body(response);
  }

  /**
   * Handles a request rejected by the service layer when trying to perform an operation on an Event
   *     which would result in the Event becoming oversubscribed.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 409 Conflict} response with the {@code CAPACITY_EXCEEDED} code
   */
  @ExceptionHandler(CapacityExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleCapacityExceededException(
      CapacityExceededException ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        ex.getMessage(),
        ApiErrorCode.CAPACITY_EXCEEDED,
        Instant.now(),
        request.getRequestURI()
    );

    return ResponseEntity.status(409).body(response);
  }

  /**
   * Handles a request rejected by the service layer when trying to perform an operation which the
   *     user does not have permission to do.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 403 Forbidden} response with the {@code USER_NOT_AUTHORISED} code
   */
  @ExceptionHandler(UserNotAuthorisedException.class)
  public ResponseEntity<ApiErrorResponse> handleUserNotAuthorisedException(
      UserNotAuthorisedException ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        ex.getMessage(),
        ApiErrorCode.USER_NOT_AUTHORISED,
        Instant.now(),
        request.getRequestURI()
    );

    return ResponseEntity.status(403).body(response);
  }

  /**
   * Handles a request rejected by the service layer when trying to perform an operation which
   * conflicts with the data stored.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 409 Conflict} response with the {@code CONFLICT} code
   */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflictException(
      ConflictException ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        ex.getMessage(),
        ApiErrorCode.CONFLICT,
        Instant.now(),
        request.getRequestURI()
    );

    return ResponseEntity.status(409).body(response);
  }

  /**
   * Handles a request rejected by the service layer because of an invalid argument.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 400 Bad Request} response with the {@code VALIDATION_FAILED} code
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        ex.getMessage(),
        ApiErrorCode.VALIDATION_FAILED,
        Instant.now(),
        request.getRequestURI()
    );

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handles a failed call across the subprocess bridge to api-core.
   *
   * <p>The response carries a fixed generic message; the exception and its cause are logged in full
   * and never returned to the caller.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 502 Bad Gateway} response with the {@code SUBPROCESS_FAILURE} code
   */
  @ExceptionHandler(SubprocessException.class)
  public ResponseEntity<ApiErrorResponse> handleSubprocessException(
      SubprocessException ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        "A downstream service failed to respond",
        ApiErrorCode.SUBPROCESS_FAILURE,
        Instant.now(),
        request.getRequestURI()
    );
    
    LOGGER.error("Subprocess exception on {}", request.getRequestURI(), ex);

    return ResponseEntity.status(502).body(response);
  }

  /**
   * Handles any exception that no more specific handler claims.
   *
   * <p>The response carries a fixed generic message; the exception is logged in full and never
   * returned to the caller.
   *
   * @param ex the thrown exception
   * @param request the request that caused the exception
   * @return a {@code 500 Internal Server Error} response with the {@code INTERNAL_ERROR} code
   */
  @ExceptionHandler
  public ResponseEntity<ApiErrorResponse> handleException(
      Exception ex, HttpServletRequest request
  ) {
    ApiErrorResponse response = new ApiErrorResponse(
        "An internal error occurred.",
        ApiErrorCode.INTERNAL_ERROR,
        Instant.now(),
        request.getRequestURI()
    );
    
    LOGGER.error("Unhandled exception on {}", request.getRequestURI(), ex);

    return ResponseEntity.internalServerError().body(response);
  }

}
