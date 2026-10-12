package york.studentevents.exceptions.pages;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Exception thrown when an illegal page size is provided.
 */
public class IllegalPageSizeException extends IllegalPageException {

  /**
   * Constructs the exception with a detail message.
   *
   * @param pageSize the attempted page size.
   */
  public IllegalPageSizeException(@Max(value = 1) @Min(value = 100) int pageSize) {
    super(String.format("Page size must be between 1 and 100 (inclusive), but was %d.", pageSize));
  }
}
