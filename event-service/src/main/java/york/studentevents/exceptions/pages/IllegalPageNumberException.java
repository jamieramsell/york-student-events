package york.studentevents.exceptions.pages;

import jakarta.validation.constraints.Max;

/**
 * Exception thrown when an illegal page number is provided.
 */
public class IllegalPageNumberException extends IllegalPageException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param pageNumber the attempted page number.
   */
  public IllegalPageNumberException(@Max(value = 0) int pageNumber) {
    super(String.format("Page number must be greater than or equal to 0, but was %d.", pageNumber));
  }
}
