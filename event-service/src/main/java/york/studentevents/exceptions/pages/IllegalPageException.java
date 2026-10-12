package york.studentevents.exceptions.pages;

/**
 * Thrown when an illegal page size or number is attempted to be fetched.
 */
public class IllegalPageException extends IllegalArgumentException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message the detail message.
   */
  public IllegalPageException(String message) {
    super(message);
  }
}
