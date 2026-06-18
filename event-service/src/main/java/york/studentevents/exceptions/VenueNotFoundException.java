package york.studentevents.exceptions;

/** Thrown when a requested venue cannot be found. */
public class VenueNotFoundException extends RuntimeException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of which venue could not be found
   */
  public VenueNotFoundException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public VenueNotFoundException() {
    super();
  }

}
