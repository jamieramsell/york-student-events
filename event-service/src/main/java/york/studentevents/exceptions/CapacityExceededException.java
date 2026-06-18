package york.studentevents.exceptions;

/**
 * Thrown when an attempt is made to add an attendee to an event that has already reached its
 * maximum capacity.
 */
public class CapacityExceededException extends RuntimeException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of why the event's capacity was exceeded
   */
  public CapacityExceededException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public CapacityExceededException() {
    super();
  }

}
