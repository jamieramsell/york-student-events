package york.studentevents.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
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
 * thrown exception, so the catch-all {@link #handleUnexpectedException} only applies to exceptions
 * that no other handler claims.
 *
 * <p>This class extends {@link ResponseEntityExceptionHandler} so that Spring MVC's own client
 * errors (unknown routes, unsupported methods, unreadable bodies, validation failures, ...) are
 * handled by its inherited handlers, not swallowed by the catch-all as a {@code 500}. Their
 * responses are rewritten into the {@link ApiErrorResponse} format by
 * {@link #handleExceptionInternal}.
 *
 * <p>Stack traces are never included in a response; where a failure is a server-side fault they
 * are written to the log instead.
 *
 * @see ApiErrorResponse
 * @see ApiErrorCode
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  
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
   * Rewrites the response for any of Spring MVC's own exceptions into the {@link ApiErrorResponse}
   * format.
   *
   * <p>Most of the inherited handlers funnel through this method. The message is taken from the
   * exception's {@link ErrorResponse} detail when it has one, as Spring writes it to be safe for
   * clients (unlike {@link Exception#getMessage()}, which can expose parser internals). Server-side
   * statuses are also logged in full.
   *
   * @param ex the thrown exception
   * @param body the body Spring would have returned; unused, as the body is rebuilt here
   * @param headers the headers Spring would have returned, e.g. {@code Allow} for a {@code 405}
   * @param code the HTTP status Spring selected
   * @param request the request that caused the exception
   * @return a response with Spring's status and headers and an {@code ApiErrorResponse} body
   */
  @Override
  public ResponseEntity<Object> handleExceptionInternal(
      @NonNull Exception ex,
      @Nullable Object body,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode code,
      @NonNull WebRequest request
  ) {
    String detail = (ex instanceof ErrorResponse errorResponse)
        ? errorResponse.getBody().getDetail()
        : null;
    String message = (detail != null) ? detail : "The request could not be processed.";
    String path = requestPath(request);

    // Log internal server errors
    if (code.is5xxServerError()) {
      LOGGER.error("Server error {} on {}", code.value(), path, ex);
    }

    ApiErrorResponse response = new ApiErrorResponse(
        message, codeFor(code), Instant.now(), path
    );

    return ResponseEntity.status(code).headers(headers).body(response);
  }

  /**
   * Handles a request body that failed Bean Validation and list each offending field.
   *
   * @param ex the thrown exception
   * @param headers the headers Spring would have returned
   * @param status the HTTP status Spring selected, {@code 400}
   * @param request the request that caused the exception
   * @return a {@code 400 Bad Request} response with the {@code VALIDATION_FAILED} code, and one
   *     entry in {@code fields} per failed field
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request
  ) {
    // Map each argument to one field
    List<ApiErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
        .toList();

    ApiErrorResponse response = new ApiErrorResponse(
        "Request validation failed",
        ApiErrorCode.VALIDATION_FAILED,
        Instant.now(),
        requestPath(request),
        fields
    );

    return ResponseEntity.status(status).headers(headers).body(response);
  }

  /**
   * Selects the error code for a status produced by Spring MVC itself.
   *
   * @param status the HTTP status
   * @return a specific code for {@code 404}, {@code 405} and {@code 415}; {@code INTERNAL_ERROR}
   *     for server errors; {@code BAD_REQUEST} for any other client error
   */
  private static ApiErrorCode codeFor(HttpStatusCode status) {
    return switch (status.value()) {
      case 404 -> ApiErrorCode.ROUTE_NOT_FOUND;
      case 405 -> ApiErrorCode.METHOD_NOT_ALLOWED;
      case 415 -> ApiErrorCode.UNSUPPORTED_MEDIA_TYPE;
      default -> status.is5xxServerError() ? ApiErrorCode.INTERNAL_ERROR : ApiErrorCode.BAD_REQUEST;
    };
  }

  /**
   * Extracts the request path from a {@code WebRequest}.
   *
   * @param request the request that caused the exception
   * @return the request URI, or {@code null} if the request is not a servlet request
   */
  private static String requestPath(WebRequest request) {
    return (request instanceof ServletWebRequest servletRequest)
        ? servletRequest.getRequest().getRequestURI()
        : null;
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
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
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
