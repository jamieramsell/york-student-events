package york.studentevents.exceptions;

/**
 * Thrown when a user tries to perform an operation that is not possible by users of their role, for
 * example, if a host attempts to register for an event.
 */
public class UserNotAuthorisedException extends RuntimeException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of why the user was not authorised
   */
  public UserNotAuthorisedException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public UserNotAuthorisedException() {
    super();
  }

}
