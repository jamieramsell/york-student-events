package york.studentevents.events.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import york.studentevents.events.EventCategory;

/**
 * DTO representing a new event to be created.
 *
 * @param title The title of the event must not be blank.
 * @param category The category of the event must not be null.
 * @param capacity The maximum number of attendees for the event; -1 for unlimited, cannot be zero
 *     or less than -1.
 */
public record CreateEventDto(
    @NotBlank(message = "Title cannot be blank") String title,

    @NotNull(message = "Category cannot be null") EventCategory category,

    @NotNull(message = "Capacity cannot be null")
    @Min(value = -1, message = "Capacity must be a non-negative integer or -1 for unlimited")
    Integer capacity
) {
  /**
   * Constructor for DTO.
   *
   * @param title The title of the event.
   * @param category The category of the event {@see EventCategory}.
   * @param capacity The capacity of the event cannot be zero or less than -1,
   *                 -1 for unlimited.
   */
  public CreateEventDto {
    if (capacity != null) {
      if (capacity == -1) {
        capacity = null;
      }
    }
  }
}
