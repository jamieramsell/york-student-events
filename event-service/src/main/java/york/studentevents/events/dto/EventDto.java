package york.studentevents.events.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import york.studentevents.events.EventCategory;
import york.studentevents.events.IEvent;

/**
 * Data Transfer Object for Event data.
 */
public record EventDto(
    UUID id,

    String title,

    String description,

    LocalDateTime startDateTime,

    LocalDateTime endDateTime,

    UUID venueId,

    Integer capacity,

    EventCategory category
) {
  /**
   * Constructor for DTO.
   *
   * @param id The event ID.
   * @param title The title of the event.
   * @param description The description of the event.
   * @param startDateTime The start date/time of the event.
   * @param endDateTime The end date/time of the event.
   * @param venueId The venue ID of the event.
   * @param capacity The capacity of the event (or -1 for unlimited) cannot be
   *                 zero or less than -1.
   * @param category The category of the event {@see EventCategory}.
   */
  public EventDto {
    if (capacity == null) {
      capacity = -1;
    }
    if (startDateTime != null && endDateTime != null) {
      if (startDateTime.isBefore(endDateTime)) {
        throw new IllegalArgumentException("Start date/time must be before end date/time");
      }
    }
  }

  /**
   * Maps an {@link IEvent} entity instance to an {@code EventDTO}.
   *
   * @param event the event entity to map.
   * @return a new EventDTO containing the entity's data.
   */
  public static EventDto fromEntity(IEvent event) {
    if (event == null) {
      return null;
    }

    return new EventDto(
        event.getId(),
        event.getTitle(),
        event.getDescription(),
        event.getStartDateTime(),
        event.getEndDateTime(),
        event.getVenue(), // Maps to the UUID venueId field
        event.getCapacity(),
        event.getCategory()
    );
  }
}