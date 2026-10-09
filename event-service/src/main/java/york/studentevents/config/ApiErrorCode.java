package york.studentevents.config;

/**
 * Machine-readable error codes returned in the {@code code} field of an {@link ApiErrorResponse}.
 *
 * <p>These let API clients branch on a stable identifier (e.g. {@code EVENT_NOT_FOUND}) instead of
 * parsing the human-readable message. Each code is defined once here so that it is never
 * stringly-typed elsewhere in the codebase.
 *
 * <p>The mapping from exceptions to codes and HTTP statuses lives in the {@code @ControllerAdvice},
 * not in this enum.
 *
 * @see ApiErrorResponse
 */
public enum ApiErrorCode {

  /** The requested event does not exist. */
  EVENT_NOT_FOUND,

  /** The requested user does not exist. */
  USER_NOT_FOUND,

  /** The requested venue does not exist. */
  VENUE_NOT_FOUND,

  /** The requested cohort does not exist. */
  COHORT_NOT_FOUND,

  /** The operation would exceed an event's (or venue's) maximum capacity. */
  CAPACITY_EXCEEDED,

  /** The user does not have permission to perform that request. */
  USER_NOT_AUTHORISED,
  
  /** The given event requires a venue for this operation, but does not yet have one. */
  MISSING_VENUE,

  /** 
   * The requested change conflicts with the current state of the system, such as adding
   * something that already exists, or removing something that is not there.
   */
  CONFLICT,

  /** The request body or parameters failed validation; field-level detail may be included. */
  VALIDATION_FAILED,

  /** An unexpected failure with the subprocess bridge between the two services. */
  SUBPROCESS_FAILURE,

  /** An unexpected server-side failure. Details are logged and never returned to the caller. */
  INTERNAL_ERROR

}
