package york.studentevents.exceptions;

/**
 * Thrown when a requested change conflicts with the current state of the system, such as adding
 * something that already exists, or removing something that is not there.
 */
public class ConflictException extends RuntimeException {
  
  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of why the conflict has occurred
   */
  public ConflictException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public ConflictException() {
    super();
  }

}
