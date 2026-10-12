package york.studentevents.exceptions;

/** Thrown when a requested cohort cannot be found. */
public class CohortNotFoundException extends EntityNotFoundException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param message a description of which cohort could not be found
   */
  public CohortNotFoundException(String message) {
    super(message);
  }

  /** Constructs the exception with no detail message. */
  public CohortNotFoundException() {
    super();
  }

}
