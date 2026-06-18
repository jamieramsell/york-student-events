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
  private String category;

  /**
   * Creates an {@code Event} with a maximum attendee capacity.
   *
   * <p>All other fields are assigned via setters after instantiation.
   *
   * @param title the event title; must not be {@code null}
   * @param capacity the maximum number of attendees; must be greater than zero
   * @param category the event category; must not be {@code null}
   * @throws IllegalArgumentException if {@code title} or {@code category} is {@code null}, or
   *     {@code capacity} is less than one
   */
  public Event(String title, int capacity, String category) {
    // Validation //
    if (title == null) {
      throw new IllegalArgumentException("title cannot be null");
    } else if (category == null) {
      throw new IllegalArgumentException("category cannot be null");
    } else if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be greater than 1");
    }

    this.id = UUID.randomUUID();
    this.title = title;
    this.capacity = Integer.valueOf(capacity);
    this.category = category;
  }

  /**
   * Creates an {@code Event} with no maximum attendee capacity.
   *
   * <p>All other fields are assigned via setters after instantiation.
   *
   * @param title the event title; must not be {@code null}
   * @param category the event category; must not be {@code null}
   * @throws IllegalArgumentException if {@code title} or {@code category} is {@code null}
   */
  public Event(String title, String category) {
    // Validation //
    if (title == null) {
      throw new IllegalArgumentException("title cannot be null");
    } else if (category == null) {
      throw new IllegalArgumentException("category cannot be null");
    }

    this.id = UUID.randomUUID();
    this.title = title;
    this.capacity = null;
    this.category = category;
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
  public String getCategory() { // todo: implement categories
    return category;
  }

  // Setters //

  @Override
  public void setTitle(String title) {
    if (title == null || title.isBlank() || title.isEmpty()) {
      throw new IllegalArgumentException("title cannot be blank, empty, or null");
    }
    this.title = title;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public void setDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime) {
    if (this.location == null) {
      throw new IllegalStateException("The event must have a location in order to assign a date and"
          + " time");
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
  
  @Override
  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  @Override
  public void setCategory(String category) { // todo: implement categories
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
