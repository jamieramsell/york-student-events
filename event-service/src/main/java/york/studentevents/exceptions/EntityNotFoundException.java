package york.studentevents.exceptions;

/**
 * Thrown when a requested entity cannot be found.
 *
 * <p>Callers should prefer using a domain-specific exception, such as
 *     {@code EventNotFoundException}.
 */
public class EntityNotFoundException extends RuntimeException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of which entity could not be found
   */
  public EntityNotFoundException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public EntityNotFoundException() {
    super();
  }

}
