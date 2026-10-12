package york.studentevents.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * The standardised JSON body returned for every failed API request.
 *
 * <p>The {@code fields} component is only populated for validation failures; when it is null or
 * empty it is omitted from the serialised JSON entirely, so all other errors keep the four-key
 * shape.
 *
 * @param error a human-readable description of what went wrong; never contains a stack trace
 * @param code the machine-readable error code clients can branch on
 * @param timestamp the moment the error occurred, serialised as an ISO-8601 UTC instant
 * @param path the path of the request that failed
 * @param fields field-level validation failures, or {@code null} when not applicable
 * @see ApiErrorCode
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    String error,
    ApiErrorCode code,
    Instant timestamp,
    String path,
    List<FieldError> fields
) {

  /**
   * A single field-level validation failure.
   *
   * @param field the name of the field that failed validation
   * @param message a human-readable description of why it failed
   */
  public record FieldError(String field, String message) {}

  /** Constructs a new standardised error response. */
  public ApiErrorResponse {
    // Copies the list of fields to prevent mutation
    fields = (fields == null) ? null : List.copyOf(fields);
  }

  /**
   * Convenience constructor for errors with no field-level detail.
   *
   * @param error a human-readable description of what went wrong
   * @param code the machine-readable error code
   * @param timestamp the moment the error occurred
   * @param path the path of the request that failed
   */
  public ApiErrorResponse(String error, ApiErrorCode code, Instant timestamp, String path) {
    this(error, code, timestamp, path, null);
  }

}