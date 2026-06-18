package york.studentevents.exceptions;

/** Thrown when a requested event cannot be found. */
public class EventNotFoundException extends RuntimeException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of which event could not be found
   */
  public EventNotFoundException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public EventNotFoundException() {
    super();
  }

}
