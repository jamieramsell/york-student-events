package york.studentevents.events;

import java.time.LocalDateTime;
import java.util.UUID;

/** Concrete implementation of {@link IEvent} representing a social event. */
public class Event implements IEvent {
  
  private UUID id;
  private String title;
  private String description;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;
  private String location;
  private Integer capacity;
  private EventCategory category;

  /**
   * Creates an {@code Event} with a maximum attendee capacity.
   *
   * @param title the event title; must not be {@code null}.
   * @param capacity the maximum number of attendees must be greater than zero.
   * @param category the event category; must not be {@code null}.
   *
   * @throws IllegalArgumentException if any of the parameters are {@code null}, or
   *      if {@code capacity} is less than one.
   */
  public Event(String title, int capacity, EventCategory category) {
    this(UUID.randomUUID(), title, capacity, category);
  }

  /**
   * Creates an {@code Event} with a maximum attendee capacity.
   *
   * @param id the event ID; must not be {@code null}.
   * @param title the event title; must not be {@code null}.
   * @param capacity the maximum number of attendees must be greater than zero.
   * @param category the event category; must not be {@code null}.
   *
   * @throws IllegalArgumentException if any of the parameters are {@code null}, or
   *      if {@code capacity} is less than one.
   */
  protected Event(UUID id, String title, int capacity, EventCategory category) {
    if (id == null) {
      throw new IllegalArgumentException("Event ID cannot be null");
    }
    this.id = id;
    setTitle(title);
    setCapacity(capacity);
    setCategory(category);
  }

  /**
   * Creates an {@code Event} without a maximum attendee capacity.
   *
   * @param id the event ID; must not be {@code null}.
   * @param title the event title; must not be {@code null}.
   * @param category the event category; must not be {@code null}.
   *
   * @throws IllegalArgumentException if any of the parameters are {@code null}, or
   *      if {@code capacity} is less than one.
   * */
  protected Event(UUID id, String title, EventCategory category) {
    if (id == null) {
      throw new IllegalArgumentException("Event ID cannot be null");
    }
    this.id = id;
    setTitle(title);
    setCategory(category);
  }


  /**
   * Creates an {@code Event} without a maximum attendee capacity.
   *
   * @param title the event title; must not be {@code null}.
   * @param category the event category; must not be {@code null}.
   *
   * @throws IllegalArgumentException if any of the parameters are {@code null}, or
   *      if {@code capacity} is less than one.
   * */
  public Event(String title, EventCategory category) {
    this(UUID.randomUUID(), title, category);
  }

  // Getters //

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  @Override
  public LocalDateTime getEndDateTime() {
    return endDateTime;
  }

  @Override
  public String getLocation() { // todo: implement venues
    return location;
  } 
  
  @Override
  public Integer getCapacity() {
    return capacity;
  }

  @Override
  public EventCategory getCategory() { // todo: implement categories
    return category;
  }

  // Setters //

  /**
   * Sets the title of this event.
   *
   * @param title the new title; must not be {@code null} or blank.
   *
   * @throws IllegalArgumentException if the title is invalid.
   */
  @Override
  public void setTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title cannot be null, empty, or blank");
    }
    this.title = title;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public void setDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime) {

    // Validation
    if (this.location == null) {
      throw new IllegalStateException("The event must have a location in order to assign a date and"
          + " time");
    } else if (startDateTime.compareTo(endDateTime) >= 0) {
      throw new IllegalArgumentException("startDateTime must be before endDateTime");
    } else if (LocalDateTime.now().compareTo(startDateTime) >= 0) {
      throw new IllegalArgumentException("The event must start in the future (startDateTime cannot"
          + " be in the past)");
    }

    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
  }

  @Override
  public void setLocation(String location) { // todo: implement venues
    if ((startDateTime != null || endDateTime != null) && location == null) {
      throw new IllegalStateException("location cannot be null if the event has already been"
          + " assigned a date and time. First remove the date and time of the event in order to"
          + " remove its location.");
    }
    this.location = location;
  } 

  /**
   * Sets the maximum number of attendees for this event.
   *
   * @param capacity the attendee cap, or {@code null} for an unlimited event
   *
   * @throws IllegalArgumentException if {@code capacity} is less than one.
   */
  @Override
  public void setCapacity(Integer capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be greater than zero");
    }
    this.capacity = capacity;
  }

  @Override
  public void setCategory(EventCategory category) { // todo: implement categories and validation
    this.category = category;
  }

  // Object method overrides //

  @Override
  public String toString() {
    String stringOutput =
        "Event: " + title + " (" + id + ")"
        + "\nCategory: " + category
        + "\nDescription: " + description
        + "\nStarts at: " + startDateTime
        + "\nEnds at: " + endDateTime
        + "\nLocation: " + location
        + "\nMaximum attendees: " + capacity;
    return stringOutput;
  }

}
