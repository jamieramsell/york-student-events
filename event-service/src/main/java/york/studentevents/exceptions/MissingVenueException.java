package york.studentevents.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when attempting to assign event timings without a venue. */
@ResponseStatus(HttpStatus.CONFLICT)
public class MissingVenueException extends RuntimeException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of the missing venue conflict
   */
  public MissingVenueException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public MissingVenueException() {
    super();
  }
}
