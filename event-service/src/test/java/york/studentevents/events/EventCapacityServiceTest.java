package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.VenueNotFoundException;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.repository.inmemory.InMemoryVenueRepository;
import york.studentevents.subscriptions.SubscriptionService;
import york.studentevents.users.Student;
import york.studentevents.venues.Venue;
import york.studentevents.venues.VenueService;

/**
 * Tests {@link EventCapacityService} against injected {@link EventService},
 *     {@link StudentEventService}, and {@link VenueService} instances.
 *
 * <p>The service delegates persistence to the injected services' repositories, and delegates some
 *     field validation to the {@link Event} entity's setters, so these tests exercise both the
 *     service's own behaviour (lookup, error translation, persistence) and its correct wiring into
 *     the entity's validation rules.
 */
public class EventCapacityServiceTest {

  private InMemoryEventRepository eventRepository;
  private EventService eventService;

  private InMemoryVenueRepository venueRepository;
  private VenueService venueService;

  private InMemorySubscriptionRepository subscriptionRepository;
  private SubscriptionService subscriptionService;

  private InMemoryUserRepository userRepository;
  private StudentEventService studentEventService;

  private EventCapacityService eventCapacityService;

  @BeforeEach
  void setUp() {
    eventRepository = new InMemoryEventRepository();
    eventService = new EventService(eventRepository);

    venueRepository = new InMemoryVenueRepository();
    venueService = new VenueService(venueRepository);

    subscriptionRepository = new InMemorySubscriptionRepository();
    subscriptionService = new SubscriptionService(subscriptionRepository);

    userRepository = new InMemoryUserRepository();
    studentEventService = new StudentEventService(
        eventRepository, userRepository, subscriptionService, eventService
    );

    eventCapacityService = new EventCapacityService(
        eventService, studentEventService, venueService
    );
  }

  // --- constructor ---

  @Test
  void constructor_whenAnyServiceIsNull_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new EventCapacityService(null, studentEventService, venueService));
    assertThrows(IllegalArgumentException.class,
        () -> new EventCapacityService(eventService, null, venueService));
    assertThrows(IllegalArgumentException.class,
        () -> new EventCapacityService(eventService, studentEventService, null));
  }

  // --- updateEventVenue ---

  @Test
  void updateEventVenue_whenAttendeesBelowVenueCapacity_assignsAndPersistsVenue() {
    Event event = newUnlimitedEvent();
    Venue venue = newVenue(5);
    registerStudents(event.getId(), 3);

    IEvent updated = eventCapacityService.updateEventVenue(event.getId(), venue.getId());

    assertEquals(venue.getId(), updated.getVenue());
    assertEquals(venue.getId(), eventService.getEvent(event.getId()).getVenue());
  }

  @Test
  void updateEventVenue_whenAttendeesEqualVenueCapacity_assignsVenue() {
    Event event = newUnlimitedEvent();
    Venue venue = newVenue(5);
    registerStudents(event.getId(), 5);

    IEvent updated = eventCapacityService.updateEventVenue(event.getId(), venue.getId());

    assertEquals(venue.getId(), updated.getVenue());
  }

  @Test
  void updateEventVenue_whenAttendeesExceedVenueCapacity_throwsIllegalStateException() {
    Event event = newUnlimitedEvent();
    Venue venue = newVenue(3);
    registerStudents(event.getId(), 5);

    assertThrows(IllegalStateException.class,
        () -> eventCapacityService.updateEventVenue(event.getId(), venue.getId()));
  }

  @Test
  void updateEventVenue_whenVenueIsUnlimited_assignsRegardlessOfAttendees() {
    Event event = newUnlimitedEvent();
    Venue venue = newUnlimitedVenue();
    registerStudents(event.getId(), 3);

    IEvent updated = eventCapacityService.updateEventVenue(event.getId(), venue.getId());

    assertEquals(venue.getId(), updated.getVenue());
  }

  @Test
  void updateEventVenue_withNullVenueId_removesVenue() {
    Event event = newUnlimitedEvent();
    Venue venue = newVenue(5);
    eventCapacityService.updateEventVenue(event.getId(), venue.getId());

    IEvent updated = eventCapacityService.updateEventVenue(event.getId(), null);

    assertNull(updated.getVenue());
    assertNull(eventService.getEvent(event.getId()).getVenue());
  }

  @Test
  void updateEventVenue_whenEventDoesNotExist_throwsEventNotFoundException() {
    Venue venue = newVenue(5);

    assertThrows(EventNotFoundException.class,
        () -> eventCapacityService.updateEventVenue(UUID.randomUUID(), venue.getId()));
  }

  @Test
  void updateEventVenue_whenVenueDoesNotExist_throwsVenueNotFoundException() {
    Event event = newUnlimitedEvent();

    assertThrows(VenueNotFoundException.class,
        () -> eventCapacityService.updateEventVenue(event.getId(), UUID.randomUUID()));
  }

  // --- updateEventCapacity ---

  @Test
  void updateEventCapacity_whenAttendeesBelowNewCapacity_updatesAndPersistsCapacity() {
    Event event = newEvent(10);
    registerStudents(event.getId(), 3);

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), 5);

    assertEquals(5, updated.getCapacity());
    assertEquals(5, eventService.getEvent(event.getId()).getCapacity());
  }

  @Test
  void updateEventCapacity_whenAttendeesEqualNewCapacity_updatesCapacity() {
    Event event = newEvent(10);
    registerStudents(event.getId(), 3);

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), 3);

    assertEquals(3, updated.getCapacity());
  }

  @Test
  void updateEventCapacity_whenAttendeesExceedNewCapacity_throwsIllegalStateException() {
    Event event = newEvent(10);
    registerStudents(event.getId(), 4);

    assertThrows(IllegalStateException.class,
        () -> eventCapacityService.updateEventCapacity(event.getId(), 3));
  }

  @Test
  void updateEventCapacity_withNullCapacityAndNoVenue_makesEventUnlimited() {
    Event event = newEvent(10);

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), null);

    assertNull(updated.getCapacity());
    assertNull(eventService.getEvent(event.getId()).getCapacity());
  }

  @Test
  void updateEventCapacity_whenRequestedExceedsVenueCapacity_clampsToVenueCapacity() {
    Venue venue = newVenue(30);
    Event event = newEventAtVenue(50, venue.getId());

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), 100);

    assertEquals(30, updated.getCapacity());
  }

  @Test
  void updateEventCapacity_withNullCapacityAndBoundedVenue_matchesVenueCapacity() {
    Venue venue = newVenue(30);
    Event event = newEventAtVenue(50, venue.getId());

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), null);

    assertEquals(30, updated.getCapacity());
  }

  @Test
  void updateEventCapacity_whenRequestedBelowVenueCapacity_keepsRequestedCapacity() {
    Venue venue = newVenue(30);
    Event event = newEventAtVenue(50, venue.getId());

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), 20);

    assertEquals(20, updated.getCapacity());
  }

  @Test
  void updateEventCapacity_withUnlimitedVenue_leavesRequestedCapacityUnchanged() {
    Venue venue = newUnlimitedVenue();
    Event event = newEventAtVenue(50, venue.getId());

    IEvent updated = eventCapacityService.updateEventCapacity(event.getId(), 40);

    assertEquals(40, updated.getCapacity());
  }

  @Test
  void updateEventCapacity_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> eventCapacityService.updateEventCapacity(UUID.randomUUID(), 50));
  }

  // --- helpers ---

  private Event newEvent(int capacity) {
    Event event = new Event("Event", capacity, EventCategory.MUSIC);
    eventRepository.save(event);
    return event;
  }

  private Event newUnlimitedEvent() {
    Event event = new Event("Event", EventCategory.MUSIC);
    eventRepository.save(event);
    return event;
  }

  private Event newEventAtVenue(int capacity, UUID venueId) {
    Event event = new Event("Event", capacity, EventCategory.MUSIC);
    event.setVenue(venueId);
    eventRepository.save(event);
    return event;
  }

  private Venue newVenue(int capacity) {
    Venue venue = new Venue("Venue", "University of York", capacity);
    venueRepository.save(venue);
    return venue;
  }

  private Venue newUnlimitedVenue() {
    Venue venue = new Venue("Venue", "University of York");
    venueRepository.save(venue);
    return venue;
  }

  private Student newStudent() {
    Student student = new Student("student", "student@york.ac.uk", "hash", new HashSet<>());
    userRepository.save(student);
    return student;
  }

  private void registerStudents(UUID eventId, int count) {
    for (int i = 0; i < count; i++) {
      studentEventService.registerForEvent(newStudent().getId(), eventId);
    }
  }
}
