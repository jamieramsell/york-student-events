package york.studentevents.events.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import york.studentevents.events.EventCategory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a partial update to an event.
 *
 * <p> {@code null} values represent unchanged fields.
 *
 * <p> If `startDateTime` is provided, `endDateTime` must also be provided and vice versa.
 * Also, `venueId` must be valid to set `startDateTime` and `endDateTime`.
 *
 * @param title The new title of the event cannot be blank.
 * @param description The new description of the event cannot be blank.
 * @param category The new category of the event must be a valid {@code EventCategory}.
 * @param capacity The new capacity of the event cannot be zero or less than -1,
 *                 -1 for unlimited.
 * @param startDateTime The new start date/time of the event must be before the end date/time.
 * @param endDateTime The new end date/time of the event must be after the start date/time.
 * @param venueId The new venue ID of the event must be a valid venue UUID.
 */
public record PatchEventDTO(
    @Size(min = 1, message = "Title cannot be empty or blank")
    String title,

    @Size(min = 1, message = "Title cannot be empty or blank")
    String description,

    EventCategory category,

    @Min(value = -1, message = "Capacity must be a non-negative integer or -1 for unlimited")
    Integer capacity,

    LocalDateTime startDateTime,

    LocalDateTime endDateTime,

    UUID venueId
) {
  /**
   * Constructor for PatchEventDTO.
   *
   * <p> {@code null} values represent unchanged fields.
   *
   * <p> If `startDateTime` is provided, `endDateTime` must also be provided and vice versa.
   * Also, `venueId` must be valid to set `startDateTime` and `endDateTime`.
   *
   * @param title The new title of the event cannot be blank.
   * @param description The new description of the event cannot be blank.
   * @param category The new category of the event must be a valid {@code EventCategory}.
   * @param capacity The new capacity of the event cannot be zero or less than -1,
   *  *                 -1 for unlimited.
   * @param startDateTime The new start date/time of the event must be before the end date/time.
   * @param endDateTime The new end date/time of the event must be after the start date/time.
   * @param venueId The new venue ID of the event must be a valid venue UUID.
   */
  public PatchEventDTO {
    if (capacity != null) {
      if (capacity == -1) {
        capacity = null;
      }
    }
    if (startDateTime != null && endDateTime != null) {
      if (startDateTime.isAfter(endDateTime)) {
        throw new IllegalArgumentException("Start date/time must be before end date/time");
      }
    } else if(startDateTime != null || endDateTime != null){
      throw new IllegalArgumentException("Start date/time and end date/time must both be provided");
    }
  }
}
