package york.studentevents.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

/** Concrete implementation of {@link IEvent} representing a social event. */
@Entity
public class Event implements IEvent {
  
  @Id
  private UUID id;

  @Column(nullable = false)
  private String title;

  private String description;

  private LocalDateTime startDateTime;

  private LocalDateTime endDateTime;

  private UUID venueId;

  private Integer capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
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
   * @throws IllegalArgumentException if any of the parameters are {@code null}
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
   * @throws IllegalArgumentException if any of the parameters are {@code null}
   * */
  public Event(String title, EventCategory category) {
    this(UUID.randomUUID(), title, category);
  }

  /** No-args constructor for JPA use only. */
  protected Event() {}

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
  public UUID getVenue() {
    return venueId;
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

    // Check that either both OR neither times have been provided: you cannot only give one.
    if (
        (startDateTime != null && endDateTime == null)
        || (endDateTime != null && startDateTime == null)
    ) {
      throw new IllegalArgumentException("You must either provide both a startDateTime as well as"
          + " an endDateTime, or provide neither. An event cannot start but not end, nor can it"
          + " end without having started.");
    } 
    
    // If times have been provided, validate that a Venue has been assigned, and that the timings
    // are correctly ordered.
    if (startDateTime != null && endDateTime != null) {
      if (this.venueId == null) {
        throw new IllegalStateException("The event must have been assigned a Venue in order to"
            + " assign a date and time");
      } else if (startDateTime != null && startDateTime.compareTo(endDateTime) >= 0) {
        throw new IllegalArgumentException("startDateTime must be before endDateTime");
      } else if (LocalDateTime.now().compareTo(startDateTime) >= 0) {
        throw new IllegalArgumentException("The event must start in the future (startDateTime"
            + " cannot be in the past)");
      }
    }

    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
  }

  @Override
  public void setVenue(UUID venueId) {
    if ((startDateTime != null || endDateTime != null) && venueId == null) {
      throw new IllegalStateException("venueId cannot be null if the event has already been"
          + " assigned a date and time. First remove the date and time of the event in order to"
          + " remove its venue.");
    }
    this.venueId = venueId;
  } 

  @Override
  public void setCapacity(Integer capacity) {
    if (capacity != null && capacity < 1) {
      throw new IllegalArgumentException("capacity must be greater than zero");
    }
    this.capacity = capacity;
  }

  @Override
  public void setCategory(EventCategory category) { // todo: implement categories and validation
    if (category == null) {
      throw new IllegalArgumentException("category cannot be null");
    }
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
        + "\nVenue ID: " + venueId
        + "\nMaximum attendees: " + capacity;
    return stringOutput;
  }

}
