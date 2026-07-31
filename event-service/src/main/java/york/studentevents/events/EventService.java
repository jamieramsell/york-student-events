package york.studentevents.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import york.studentevents.exceptions.EventNotFoundException;

/**
 * Application service exposing event-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * {@link EventController} and the {@link IEventRepository}. Persistence is delegated to the
 * injected repository; the service holds no state of its own.
 *
 * @see IEventRepository
 * @see IEvent
 */
public class EventService {

  private IEventRepository repository;

  /**
   * Constructs an {@code EventService} backed by the given repository.
   *
   * @param repositoryInjection the repository used to store and retrieve events; must not be
   *     {@code null}
   */
  public EventService(IEventRepository repositoryInjection) {
    if (repositoryInjection == null) {
      throw new IllegalArgumentException("repositoryInjection must not be null");
    }
    this.repository = repositoryInjection;
  }

  /**
   * Retrieves an event from the injected EventRepository.
   *
   * @param eventId the event's ID
   * @return the {@code IEvent} entity; never null
   * @throws EventNotFoundException if the given event does not exist
   */
  public IEvent getEvent(UUID eventId) {
    Optional<IEvent> optionalEvent = repository.findByID(eventId);
    if (optionalEvent.isEmpty()) {
      throw new EventNotFoundException();
    }
    return optionalEvent.get();
  }

  /**
   * Creates and stores a record of an event.
   *
   * @param title the event's title; must not be null.
   * @param category the event's category; must not be null.
   * @param capacity the (optional) maximum capacity of the event.
   * @return a copy of the created {@code IEvent} entity.
   *
   * @throws IllegalArgumentException if any non-nullable argument is null, or if a capacity is
   *     given which is less than 1.
   */
  public IEvent createEvent(String title, EventCategory category, Integer capacity) {
    IEvent event;

    if (capacity == null) {
      event = new Event(title, category);
    } else {
      event = new Event(title, capacity, category);
    }

    repository.save(event);
    return event;
  }

  /**
   * Retrieves an Event record, and updates its title field.
   *
   * @param id The ID of the target event.
   * @param title The new event title; must not be null or empty.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   * @throws IllegalArgumentException if title is null or empty.
   */
  public IEvent updateEventTitle(UUID id, String title) {
    IEvent event = getEvent(id);
    event.setTitle(title);
    repository.save(event);
    return event;
  }

  /**
   * Retrieves an Event record, and updates its description field.
   *
   * <p>Passing a {@code null} description will remove the description from the Event.
   *
   * @param id The ID of the target event.
   * @param description The new event description.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   */
  public IEvent updateEventDescription(UUID id, String description) {
    IEvent event = getEvent(id);
    event.setDescription(description);
    repository.save(event);
    return event;
  }

  /**
   * Retrieves an Event record, and updates its start and end datetime.
   *
   * <p>Note that a location must have first been assigned to the event, in order to assign it a
   *     date and time.
   *
   * <p>In order to remove the prescribed start and end datetimes of an event, you must pass both
   *     parameters as {@code null}.
   *
   * @param id The ID of the target event.
   * @param startDateTime when the event begins.
   * @param endDateTime when the event ends.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   * @throws IllegalStateException if trying to assign event timings, despite the event not yet
   *     having an assigned location.
   * @throws IllegalArgumentException if {@code endDateTime} is before {@code startDateTime}, or if
   *     {@code startDateTime} is in the past.
   * @throws IllegalArgumentException if only one datetime is provided. You must either provide both
   *     in order to assign event timings, or neither to remove them.
   */
  public IEvent updateEventDateTime(
      UUID id,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime
  ) {
    IEvent event = getEvent(id);
    event.setDateTime(startDateTime, endDateTime);
    repository.save(event);
    return event;
  }

  /**
   * Retrieves an Event record, and updates its location field.
   *
   * <p>Note that passing a {@code null} location will both the location, as well as the event's
   *     start and end timings, as an event must have a location in order to have assigned start /
   *     end timings.
   *
   * @param id The ID of the target event.
   * @param location The (optional) new event location.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   */
  public IEvent updateEventLocation(UUID id, String location) {
    IEvent event = getEvent(id);
    event.setDateTime(null, null);
    event.setLocation(location);
    repository.save(event);
    return event;
  }

  /**
   * Retrieves an Event record, and updates its capacity field.
   *
   * <p>Passing a {@code null} capacity will remove its prescribed maximum capacity.
   *
   * @param id The ID of the target event.
   * @param capacity The (optional) new event capacity.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   */
  public IEvent updateEventCapacity(UUID id, Integer capacity) {
    IEvent event = getEvent(id);
    event.setCapacity(capacity);
    repository.save(event);
    return event;
  }

  /**
   * Retrieves an Event record, and updates its category field.
   *
   * @param id The ID of the target event.
   * @param category The new event category; must not be null.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   */
  public IEvent updateEventCategory(UUID id, EventCategory category) {
    IEvent event = getEvent(id);
    event.setCategory(category);
    repository.save(event);
    return event;
  }

  /**
   * Retrieves every event currently held by the backing repository.
   *
   * @return a {@link List} of all events; never {@code null}, but may be empty if no events have
   *     been saved
   */
  public List<IEvent> getAllEvents() {
    return repository.findAll();
  }

  /**
   * Retrieves all events which fall under any of the given set of categories.
   *
   * @param categories The set of categories to retrieve
   * @return a {@link List} of all events; never {@code null}, but may be empty if no events have
   *     been saved
   *
   * @see EventCategory
   */
  public List<IEvent> getEventsByCategory(Set<EventCategory> categories) {
    Predicate<IEvent> isRelevant = (IEvent event) -> categories.contains(event.getCategory());
    return getAllEvents()
        .stream()
        .filter(isRelevant)
        .toList();
  }

  /**
   * Retrieves all events which take place between two specified points in time. These two
   * boundaries are inclusive, so an event which starts on the {@code start} boundary for example
   * will be retrieved.
   *
   * <p>The datetime boundaries are nullable, meaning you can have a bound which an event must start
   *     at or after, without a restriction on when the event must finish, and vice versa.
   *
   * @param start The (optional) beginning of the time window
   * @param end The (optional) end of the time window
   * @return All events which lie between the given points in time, inclusive.
   */
  public List<IEvent> getEventsByDateTime(LocalDateTime start, LocalDateTime end) {
    return getAllEvents()
        .stream()
        .filter(event -> eventFallsBetween(event, start, end))
        .toList();
  }

  /**
   * Helper method which checks whether a given event lies wholly between two points in time. This
   * check is inclusive of either boundary.
   *
   * <p>{@code start} and {@code end} are nullable, which can be used if wanting to check whether an
   *     event takes place before or after a given point, without enforcing a bound on the other
   *     side.
   *
   * @param event The event to check
   * @param start The (optional) beginning of the time window
   * @param end The (optional) end of the time window
   * @return Whether or not the event falls between the two specified points in time
   */
  private boolean eventFallsBetween(IEvent event, LocalDateTime start, LocalDateTime end) {
    if (start != null) {
      if (event.getStartDateTime() == null) {
        return false;
      } else if (
            !(event.getStartDateTime().isAfter(start)
            || event.getStartDateTime().isEqual(start))
      ) {
        return false;
      }
    }

    if (end != null) {
      if (event.getEndDateTime() == null) {
        return false;
      } else if (
          !(event.getEndDateTime().isBefore(end)
          || event.getEndDateTime().isEqual(end))
      ) {
        return false;
      }
    }

    // If this point is reached, then the event does lie within the two points (inclusively)
    return true;
  }

  /**
   * Deletes an event from the injected EventRepository.
   *
   * @param eventId the event's ID
   * @throws EventNotFoundException if the given event does not exist
   */
  public void deleteEvent(UUID eventId) {
    try {
      repository.delete(eventId);
    } catch (NoSuchElementException e) {
      throw new EventNotFoundException();
    }
  }

}
