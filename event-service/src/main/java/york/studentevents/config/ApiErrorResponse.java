package york.studentevents.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    String error,
    ApiErrorCode code,
    Instant timestamp,
    String path,
    List<FieldError> fields
) {

  /** A single field-level validation failure. */
  public record FieldError(String field, String message) {}

  /** Constructs a new standardised error response. */
  public ApiErrorResponse {
    // Copies the list of fields to prevent mutation
    fields = (fields == null) ? null : List.copyOf(fields);
  }

  /** Convenience constructor for errors with no field-level detail. */
  public ApiErrorResponse(String error, ApiErrorCode code, Instant timestamp, String path) {
    this(error, code, timestamp, path, null);
  }

}