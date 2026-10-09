package york.studentevents.exceptions;

/** Thrown when a requested user cannot be found. */
public class UserNotFoundException extends EntityNotFoundException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of which user could not be found
   */
  public UserNotFoundException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public UserNotFoundException() {
    super();
  }

}
