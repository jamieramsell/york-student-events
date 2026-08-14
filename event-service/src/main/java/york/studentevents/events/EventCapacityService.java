package york.studentevents.events;

import java.util.UUID;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.VenueNotFoundException;
import york.studentevents.venues.IVenue;
import york.studentevents.venues.VenueService;

/**
 * Thin application service exposing capacity-related Event operations.
 *
 * <p>This service forms part of the {@code Service} layer of the stack, sitting between the
 * controllers and repositories. Persistence is delegated to the injected services, as this thin
 * service holds no state of its own.
 *
 * @see EventService
 * @see VenueService
 * @see IEvent
 * @see IVenue
 */
public class EventCapacityService {

  private final EventService eventService;
  private final StudentEventService studentEventService;
  private final VenueService venueService;

  /**
   * Constructor for EventCapacityService.
   *
   * @param eventService the current event service instance
   * @param studentEventService the current student event service instance
   * @param venueService the current venue service instance
   */
  public EventCapacityService(
      EventService eventService,
      StudentEventService studentEventService,
      VenueService venueService
  ) {
    if (eventService == null || studentEventService == null || venueService == null) {
      throw new IllegalArgumentException("Injected services cannot be null");
    }
    this.eventService = eventService;
    this.studentEventService = studentEventService;
    this.venueService = venueService;
  }

  /**
   * Retrieves an Event record, and updates its Venue field.
   *
   * <p>Assigning a Venue to the Event may reduce its total capacity. It is important to check that
   *     the capacity of the Venue is compatible with that of the Event.
   *
   * <p>It is not possible to assign a Venue to an Event if assigning that Venue will cause the
   *     Event to become oversubscribed. If it is absolutely necessary to assign a Venue with a
   *     lower capacity than the number of attendees, then you must first remove a sufficient number
   *     of attendees before assigning the new Venue. This can be done via the
   *     {@link StudentEventService}.
   *
   * <p>Note that passing a {@code null} venue ID will remove both the event's Venue, as well as
   *     its start and end timings. An event must have a Venue in order to have been assigned
   *     start / end timings.
   *
   * @param eventId The ID of the target event.
   * @param venueId The (optional) ID of the Venue where the Event is to take place.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an Event with the given ID could not be found.
   * @throws VenueNotFoundException if a Venue with the given ID could not be found.
   * @throws IllegalStateException if the Event has more attendees than the capacity of the Venue.
   */
  public IEvent updateEventVenue(UUID eventId, UUID venueId) {
    eventService.getEvent(eventId); // Validate event existence first
    IVenue venue = venueService.getVenue(venueId);
    int attendees = studentEventService.countRegisteredStudents(eventId);

    if (attendees > venue.getCapacity()) {
      throw new IllegalStateException("The given Event has more attendees enrolled than the"
        + " capacity of the Venue. To assign this Venue, you must first unassign some Students.");
    }
    
    return eventService.updateEventVenue(eventId, venueId);
  }

  /**
   * Retrieves an Event record, and updates its capacity field.
   *
   * <p>If attempting to assign a capacity which exceeds that of the Event's Venue, the service will
   *     still attempt to increase the capacity of the Event by matching it to that of the Venue, 
   *     therefore it is important to check that the capacity of the Venue is compatible with that
   *     of the Event.
   *
   * <p>It is not possible to reduce the capacity of an Event if it will cause the Event to become
   *     oversubscribed. If it is absolutely necessary to assign a lower capacity than the number of
   *     attendees, then you must first remove a sufficient number of attendees before assigning the
   *     new capacity. This can be done via the {@link StudentEventService}.
   * 
   * <p>Passing a {@code null} capacity will attempt to maximise the Event's capacity, which means
   *     that it will be matched to that of its Venue if it has been assigned with one.
   *
   * @param id The ID of the target event.
   * @param capacity The (optional) new event capacity.
   * @return A copy of the updated event record.
   *
   * @throws EventNotFoundException if an event with the given ID could not be found.
   * @throws IllegalStateException if the Event has more attendees than the given capacity allows.
   */
  public IEvent updateEventCapacity(UUID id, Integer capacity) {
    IEvent event = eventService.getEvent(id);
    int attendees = studentEventService.countRegisteredStudents(id);

    UUID venueId = event.getVenue();
    IVenue venue = null;
    if (venueId != null) {
      venue = venueService.getVenue(venueId);
    }

    if (capacity != null && attendees > capacity) {
      throw new IllegalStateException("The given Event has more attendees enrolled than the"
        + " given capacity. To assign this capacity, you must first unassign some Students.");
    }
    
    // Must manually overwrite the repository record, rather than routing through EventService, as
    // the EventService does not expose a setCapacity method.

    // Capacity ceiling = venue capacity
    if (venue != null && venue.getCapacity() != null) { 
      if (capacity == null || capacity > venue.getCapacity()) {
        capacity = venue.getCapacity();
      }
    }

    return eventService.updateEventCapacity(id, capacity);
  }
}
