package york.studentevents.events;

import java.time.LocalDateTime;
import java.util.UUID;
import york.studentevents.exceptions.MissingVenueException;
import york.studentevents.repository.IEntity;

/** Represents a social event that can be attended by students. */
public abstract class IEvent extends IEntity {

  // Getters //

  /** Returns the title of this event. */
  public abstract String getTitle();

  /** Returns an extended description of this event. */
  public abstract String getDescription();

  /** Returns the date and time at which this event begins. */
  public abstract LocalDateTime getStartDateTime();

  /** Returns the date and time at which this event ends. */
  public abstract LocalDateTime getEndDateTime();

  /** Returns the ID of the venue which is hosting the event.
   *
   * <p>Note that validation that the given venue exists must be handled by the EventService. This
   *     validation is not handled within the Event domain itself.
   */
  public abstract UUID getVenue(); 

  /**
   * Returns the maximum number of attendees for this event, or {@code null} if there is no limit.
   *
   * <p>Note that validation that the capacity of the event does not exceed that of its venue must
   *     be handled by the EventService. This validation is not handled within the Event domain
   *     itself.
   */
  public abstract Integer getCapacity();

  /**
   * Returns the category that classifies this event.
   *
   * @see EventCategory
   */
  public abstract EventCategory getCategory();

  // Setters //

  /**
   * Sets the title of this event.
   *
   * @param title the new title; must not be {@code null} or blank
   */
  public abstract void setTitle(String title);

  /**
   * Sets the description of this event.
   *
   * @param description a human-readable summary of the event
   */
  public abstract void setDescription(String description);

  /**
   * Sets the start and end date/time for this event.
   *
   * <p>An event can only be assigned a date and time once it has been given a venue.
   *
   * <p>A start and end time of {@code null} will simply remove the event's current datetime, if it
   *     already has one.
   *
   * @param startDateTime when the event begins
   * @param endDateTime when the event ends; must not be {@code null} if a {@code startDateTime} has
   *     been provided, and must occur after {@code startDateTime}
   * @throws MissingVenueException if no location has been assigned to the event.
   * @throws IllegalArgumentException if {@code endDateTime} is before {@code startDateTime}, or if
   *     an {@code endDateTime} has been provided when {@code startDateTime == null}.
   */
  public abstract void setDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime);

  /**
   * Sets the venue of this event.
   *
   * <p>Note that validation that the given venue exists, and that the capacity of the event does
   *     not exceed that of its venue, must be handled by the EventService. This validation is not
   *     handled within the Event domain itself.
   *
   * @param venueId the ID of the venue where the event takes place
   * @throws IllegalStateException if trying to remove the location when the event has already been
   *     assigned a date and time.
   */
  public abstract void setVenue(UUID venueId);

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
  public abstract void setCapacity(Integer capacity);

  /**
   * Sets the category that classifies this event.
   *
   * @param category the category label
   * 
   * @see EventCategory
   */
  public abstract void setCategory(EventCategory category);

}
