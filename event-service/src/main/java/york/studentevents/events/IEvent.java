package york.studentevents.events;

import java.time.LocalDateTime;

/** Represents a social event that can be attended by students. */
public interface IEvent {

  // Getters //

  /** Returns the unique identifier for this event. */
  long getId();

  /** Returns the title of this event. */
  String getTitle();

  /** Returns an extended description of this event. */
  String getDescription();

  /** Returns the date and time at which this event begins. */
  LocalDateTime getStartDateTime();

  /** Returns the date and time at which this event ends. */
  LocalDateTime getEndDateTime();

  /** Returns the venue which is hosting the event. */
  String getLocation(); // todo: implement venues

  /**
   * Returns the maximum number of attendees for this event, or {@code null} if there is no limit.
   */
  Integer getCapacity();

  /** Returns the category that classifies this event (e.g. "Music", "Sport", "Nightlife"). */
  String getCategory(); // todo: implement categories

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
   * <p> An event can only be assigned a date and time once it has been given a location.
   *
   * @param startDateTime when the event begins; must not be {@code null}
   * @param endDateTime when the event ends; must not be {@code null} and must be after
   * {@code startDateTime}
   * 
   * @throws IllegalStateException if no location has been assigned to the event.
   */
  void setDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime);

  /**
   * Sets the location of this event.
   * 
   * @param location the venue or address where the event takes place
   * 
   * @throws IllegalStateException if trying to remove the location when the event has already been
   * assigned a date and time.
   */
  void setLocation(String location); // todo: implement venues

  /**
   * Sets the maximum number of attendees for this event.
   *
   * @param capacity the attendee cap, or {@code null} for an unlimited event
   */
  void setCapacity(Integer capacity);

  /**
   * Sets the category that classifies this event.
   *
   * @param category the category label (e.g. "Music", "Sport", "Nightlife")
   */
  void setCategory(String category); // todo: implement categories

}
