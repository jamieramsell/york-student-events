package york.studentevents.exceptions;

/** Thrown when the Python responder returns an error envelope or exits non-zero. */
public class SubprocessException extends RuntimeException {

  /**
   * Constructs the exception with a detail message and a cause.
   *
   * @param message a description of why the {@code SubprocessException} was thrown
   * @param cause the cause of the exception
   */
  public SubprocessException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs the exception with a detail message, but no cause.
   *
   * @param message a description of why the {@code SubprocessException} was thrown
   */
  public SubprocessException(String message) {
    super(message);
  }

  /**
   * Constructs the exception with a cause, but no detail message.
   *
   * @param cause the cause of the exception
   */
  public SubprocessException(Throwable cause) {
    super(cause);
  }

  /** Constructs the exception with no detail message or cause. */
  public SubprocessException() {
    super();
  }

}
