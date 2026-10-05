package york.studentevents.events.dto;

import york.studentevents.events.EventCategory;
import york.studentevents.events.IEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Event data.
 */
public record EventDTO(
    UUID id,

    String title,

    String description,

    LocalDateTime startDateTime,

    LocalDateTime endDateTime,

    UUID venueId,

    Integer capacity,

    EventCategory category
) {

  public EventDTO {
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
  public static EventDTO fromEntity(IEvent event) {
    if (event == null) {
      return null;
    }

    return new EventDTO(
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