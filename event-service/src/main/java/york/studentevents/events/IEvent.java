package york.studentevents.events;

import java.time.LocalDateTime;
import java.util.UUID;

import york.studentevents.repository.IEntity;

/** Represents a social event that can be attended by students. */
public interface IEvent extends IEntity {

  // Getters //

  /** Returns the title of this event. */
  String getTitle();

  /** Returns an extended description of this event. */
  String getDescription();

  /** Returns the date and time at which this event begins. */
  LocalDateTime getStartDateTime();

  /** Returns the date and time at which this event ends. */
  LocalDateTime getEndDateTime();

  /** Returns the ID of the venue which is hosting the event. */
  UUID getVenue(); 

  /**
   * Returns the maximum number of attendees for this event, or {@code null} if there is no limit.
   */
  Integer getCapacity();

  /**
   * Returns the category that classifies this event.
   *
   * @see EventCategory
   */
  EventCategory getCategory();

  // Setters //

  /**
   * Sets the title of this event.
   *
   * @param title the new title; must not be {@code null} or blank
   */
  void setTitle(String title);

  /**
   * Sets the description of this event.
   *
   * @param description a human-readable summary of the event
   */
  void setDescription(String description);

  /**
   * Sets the start and end date/time for this event.
   *
   * <p>An event can only be assigned a date and time once it has been given a location.
   *
   * <p>A start and end time of {@code null} will simply remove the event's current datetime, if it
   *     already has one.
   *
   * @param startDateTime when the event begins
   * @param endDateTime when the event ends; must not be {@code null} if a {@code startDateTime} has
   *     been provided, and must occur after {@code startDateTime}
   * @throws IllegalStateException if no location has been assigned to the event.
   * @throws IllegalArgumentException if {@code endDateTime} is before {@code startDateTime}, or if
   *     an {@code endDateTime} has been provided when {@code startDateTime == null}.
   */
  void setDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime);

  /**
   * Sets the venue of this event.
   *
   * <p>Note that validation that the capacity of the event does not exceed that of its venue must
   *     be handled by the EventService. This validation is not handled within the Event domain
   *     itself.
   *
   * @param venueId the ID of the venue where the event takes place
   * @throws IllegalStateException if trying to remove the location when the event has already been
   *     assigned a date and time.
   */
  void setVenue(UUID venueId);

  /**
   * Sets the maximum number of attendees for this event.
   *
   * <p>Note that validation that the capacity of the event does not exceed that of its venue must
   *     be handled by the EventService. This validation is not handled within the Event domain
   *     itself.
   *
   * @param capacity the attendee cap, or {@code null} for an unlimited event
   *
   * @throws IllegalArgumentException if {@code capacity} is less than one.
   */
  void setCapacity(Integer capacity);

  /**
   * Sets the category that classifies this event.
   *
   * @param category the category label
   * 
   * @see EventCategory
   */
  void setCategory(EventCategory category);

}
