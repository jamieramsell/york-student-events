package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.repository.inmemory.InMemoryEventRepository;

/**
 * Tests {@link EventService} against a real {@link InMemoryEventRepository}.
 *
 * <p>The service delegates persistence to the repository and delegates most field validation to the
 * {@link Event} entity's setters, so these tests exercise both the service's own behaviour (lookup,
 * error translation, persistence) and its correct wiring into the entity's validation rules.
 */
class EventServiceTest {

  private InMemoryEventRepository repository;
  private EventService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventRepository();
    service = new EventService(repository);
  }

  // --- constructor ---

  @Test
  void constructor_withNullRepository_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new EventService(null));
  }

  // --- getEvent ---

  @Test
  void getEvent_whenEventExists_returnsEvent() {
    IEvent event = savedEvent();

    assertEquals(event, service.getEvent(event.getId()));
  }

  @Test
  void getEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class, () -> service.getEvent(UUID.randomUUID()));
  }

  // --- createEvent ---

  @Test
  void createEvent_withCapacity_createsAndPersistsEvent() {
    IEvent event = service.createEvent("Freshers Fair", EventCategory.FRESHERS, 200);

    assertEquals("Freshers Fair", event.getTitle());
    assertEquals(EventCategory.FRESHERS, event.getCategory());
    assertEquals(200, event.getCapacity());
    assertEquals(event, service.getEvent(event.getId()));
  }

  @Test
  void createEvent_withNullCapacity_createsUnlimitedEvent() {
    IEvent event = service.createEvent("Open Lecture", EventCategory.ACADEMIC, null);

    assertNull(event.getCapacity());
    assertEquals(event, service.getEvent(event.getId()));
  }

  @Test
  void createEvent_withNullTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createEvent(null, EventCategory.MUSIC, 10));
  }

  @Test
  void createEvent_withBlankTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createEvent("   ", EventCategory.MUSIC, 10));
  }

  @Test
  void createEvent_withNullCategory_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createEvent("Gig", null, 10));
  }

  @Test
  void createEvent_withCapacityBelowOne_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createEvent("Gig", EventCategory.MUSIC, 0));
  }

  @Test
  void createEvent_whenValidationFails_doesNotPersist() {
    assertThrows(IllegalArgumentException.class,
        () -> service.createEvent(null, EventCategory.MUSIC, 10));

    assertTrue(service.getAllEvents().isEmpty());
  }

  // --- updateEventTitle ---

  @Test
  void updateEventTitle_updatesAndPersistsTitle() {
    IEvent event = savedEvent();

    service.updateEventTitle(event.getId(), "New Title");

    assertEquals("New Title", service.getEvent(event.getId()).getTitle());
  }

  @Test
  void updateEventTitle_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> service.updateEventTitle(UUID.randomUUID(), "New Title"));
  }

  @Test
  void updateEventTitle_withNullTitle_throwsIllegalArgumentException() {
    IEvent event = savedEvent();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventTitle(event.getId(), null));
  }

  @Test
  void updateEventTitle_withBlankTitle_throwsIllegalArgumentException() {
    IEvent event = savedEvent();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventTitle(event.getId(), "   "));
  }

  // --- updateEventDescription ---

  @Test
  void updateEventDescription_updatesAndPersistsDescription() {
    IEvent event = savedEvent();

    service.updateEventDescription(event.getId(), "A great night out");

    assertEquals("A great night out", service.getEvent(event.getId()).getDescription());
  }

  @Test
  void updateEventDescription_withNull_removesDescription() {
    IEvent event = savedEvent();
    service.updateEventDescription(event.getId(), "A great night out");

    service.updateEventDescription(event.getId(), null);

    assertNull(service.getEvent(event.getId()).getDescription());
  }

  @Test
  void updateEventDescription_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> service.updateEventDescription(UUID.randomUUID(), "desc"));
  }

  // --- updateEventDateTime ---

  @Test
  void updateEventDateTime_withLocationAndValidTimes_updatesAndPersists() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    LocalDateTime end = start.plusHours(2);

    service.updateEventDateTime(event.getId(), start, end);

    IEvent fetched = service.getEvent(event.getId());
    assertEquals(start, fetched.getStartDateTime());
    assertEquals(end, fetched.getEndDateTime());
  }

  @Test
  void updateEventDateTime_withBothNull_clearsTimings() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    service.updateEventDateTime(event.getId(), start, start.plusHours(2));

    service.updateEventDateTime(event.getId(), null, null);

    IEvent fetched = service.getEvent(event.getId());
    assertNull(fetched.getStartDateTime());
    assertNull(fetched.getEndDateTime());
  }

  @Test
  void updateEventDateTime_whenNoLocationAssigned_throwsIllegalStateException() {
    IEvent event = savedEvent();
    LocalDateTime start = LocalDateTime.now().plusDays(1);

    assertThrows(IllegalStateException.class,
        () -> service.updateEventDateTime(event.getId(), start, start.plusHours(2)));
  }

  @Test
  void updateEventDateTime_whenEndBeforeStart_throwsIllegalArgumentException() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime start = LocalDateTime.now().plusDays(2);
    LocalDateTime end = LocalDateTime.now().plusDays(1);

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventDateTime(event.getId(), start, end));
  }

  @Test
  void updateEventDateTime_whenStartInPast_throwsIllegalArgumentException() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime start = LocalDateTime.now().minusDays(1);
    LocalDateTime end = LocalDateTime.now().plusDays(1);

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventDateTime(event.getId(), start, end));
  }

  @Test
  void updateEventDateTime_withOnlyStart_throwsIllegalArgumentException() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime start = LocalDateTime.now().plusDays(1);

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventDateTime(event.getId(), start, null));
  }

  @Test
  void updateEventDateTime_withOnlyEnd_throwsIllegalArgumentException() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime end = LocalDateTime.now().plusDays(1);

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventDateTime(event.getId(), null, end));
  }

  @Test
  void updateEventDateTime_whenEventDoesNotExist_throwsEventNotFoundException() {
    LocalDateTime start = LocalDateTime.now().plusDays(1);

    assertThrows(EventNotFoundException.class,
        () -> service.updateEventDateTime(UUID.randomUUID(), start, start.plusHours(2)));
  }

  // --- updateEventLocation ---

  @Test
  void updateEventLocation_updatesAndPersistsLocation() {
    IEvent event = savedEvent();

    service.updateEventLocation(event.getId(), "The Courtyard");

    assertEquals("The Courtyard", service.getEvent(event.getId()).getLocation());
  }

  @Test
  void updateEventLocation_clearsAnyExistingDateTime() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "Central Hall");
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    service.updateEventDateTime(event.getId(), start, start.plusHours(2));

    service.updateEventLocation(event.getId(), "The Courtyard");

    IEvent fetched = service.getEvent(event.getId());
    assertEquals("The Courtyard", fetched.getLocation());
    assertNull(fetched.getStartDateTime());
    assertNull(fetched.getEndDateTime());
  }

  @Test
  void updateEventLocation_withNull_removesLocation() {
    IEvent event = savedEvent();
    service.updateEventLocation(event.getId(), "The Courtyard");

    service.updateEventLocation(event.getId(), null);

    assertNull(service.getEvent(event.getId()).getLocation());
  }

  @Test
  void updateEventLocation_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> service.updateEventLocation(UUID.randomUUID(), "The Courtyard"));
  }

  // --- updateEventCapacity ---

  @Test
  void updateEventCapacity_updatesAndPersistsCapacity() {
    IEvent event = savedEvent();

    service.updateEventCapacity(event.getId(), 50);

    assertEquals(50, service.getEvent(event.getId()).getCapacity());
  }

  @Test
  void updateEventCapacity_withNull_removesCapacity() {
    IEvent event = savedEventWithCapacity(50);

    service.updateEventCapacity(event.getId(), null);

    assertNull(service.getEvent(event.getId()).getCapacity());
  }

  @Test
  void updateEventCapacity_belowOne_throwsIllegalArgumentException() {
    IEvent event = savedEvent();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventCapacity(event.getId(), 0));
  }

  @Test
  void updateEventCapacity_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> service.updateEventCapacity(UUID.randomUUID(), 50));
  }

  // --- updateEventCategory ---

  @Test
  void updateEventCategory_updatesAndPersistsCategory() {
    IEvent event = savedEvent();

    service.updateEventCategory(event.getId(), EventCategory.SPORTS);

    assertEquals(EventCategory.SPORTS, service.getEvent(event.getId()).getCategory());
  }

  @Test
  void updateEventCategory_withNull_throwsIllegalArgumentException() {
    IEvent event = savedEvent();

    assertThrows(IllegalArgumentException.class,
        () -> service.updateEventCategory(event.getId(), null));
  }

  @Test
  void updateEventCategory_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> service.updateEventCategory(UUID.randomUUID(), EventCategory.SPORTS));
  }

  // --- getAllEvents ---

  @Test
  void getAllEvents_whenEmpty_returnsEmptyList() {
    assertTrue(service.getAllEvents().isEmpty());
  }

  @Test
  void getAllEvents_returnsAllSavedEvents() {
    IEvent first = savedEvent();
    IEvent second = savedEvent();

    List<IEvent> events = service.getAllEvents();

    assertEquals(2, events.size());
    assertTrue(events.contains(first));
    assertTrue(events.contains(second));
  }

  // --- deleteEvent ---

  @Test
  void deleteEvent_removesEvent() {
    IEvent event = savedEvent();

    service.deleteEvent(event.getId());

    assertThrows(EventNotFoundException.class, () -> service.getEvent(event.getId()));
  }

  @Test
  void deleteEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> service.deleteEvent(UUID.randomUUID()));
  }

  // --- getEventsByCategory ---

  @Test
  void getEventsByCategory_returnsEventsInSingleCategory() {
    IEvent music = savedEvent(EventCategory.MUSIC);
    savedEvent(EventCategory.SPORTS);
    savedEvent(EventCategory.ACADEMIC);

    List<IEvent> results = service.getEventsByCategory(Set.of(EventCategory.MUSIC));

    assertEquals(List.of(music), results);
  }

  @Test
  void getEventsByCategory_matchesAnyOfTheGivenCategories() {
    final IEvent music = savedEvent(EventCategory.MUSIC);
    final IEvent sports = savedEvent(EventCategory.SPORTS);
    savedEvent(EventCategory.ACADEMIC);

    List<IEvent> results =
        service.getEventsByCategory(Set.of(EventCategory.MUSIC, EventCategory.SPORTS));

    assertEquals(2, results.size());
    assertTrue(results.contains(music));
    assertTrue(results.contains(sports));
  }

  @Test
  void getEventsByCategory_whenNoEventMatches_returnsEmptyList() {
    savedEvent(EventCategory.MUSIC);
    savedEvent(EventCategory.SPORTS);

    assertTrue(service.getEventsByCategory(Set.of(EventCategory.NIGHTLIFE)).isEmpty());
  }

  @Test
  void getEventsByCategory_withEmptyCategorySet_returnsEmptyList() {
    savedEvent(EventCategory.MUSIC);

    assertTrue(service.getEventsByCategory(Set.of()).isEmpty());
  }

  @Test
  void getEventsByCategory_whenNoEventsSaved_returnsEmptyList() {
    assertTrue(service.getEventsByCategory(Set.of(EventCategory.MUSIC)).isEmpty());
  }

  // --- getEventsByDateTime ---

  @Test
  void getEventsByDateTime_withBothBoundsNull_returnsAllEvents() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);
    IEvent first = savedScheduledEvent(base, base.plusHours(1));
    IEvent second = savedScheduledEvent(base.plusHours(2), base.plusHours(3));

    List<IEvent> results = service.getEventsByDateTime(null, null);

    assertEquals(2, results.size());
    assertTrue(results.contains(first));
    assertTrue(results.contains(second));
  }

  @Test
  void getEventsByDateTime_withBothBoundsNull_includesUnscheduledEvents() {
    IEvent unscheduled = savedEvent(EventCategory.MUSIC); // no start/end datetime

    List<IEvent> results = service.getEventsByDateTime(null, null);

    assertTrue(results.contains(unscheduled));
  }

  @Test
  void getEventsByDateTime_withStartBoundOnly_returnsEventsStartingAtOrAfterIt() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);
    savedScheduledEvent(base.plusHours(1), base.plusHours(2)); // starts before bound
    IEvent later = savedScheduledEvent(base.plusHours(5), base.plusHours(6));

    List<IEvent> results = service.getEventsByDateTime(base.plusHours(3), null);

    assertEquals(List.of(later), results);
  }

  @Test
  void getEventsByDateTime_withEndBoundOnly_returnsEventsEndingAtOrBeforeIt() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);
    IEvent earlier = savedScheduledEvent(base.plusHours(1), base.plusHours(2));
    savedScheduledEvent(base.plusHours(5), base.plusHours(6)); // ends after bound

    List<IEvent> results = service.getEventsByDateTime(null, base.plusHours(4));

    assertEquals(List.of(earlier), results);
  }

  @Test
  void getEventsByDateTime_withBothBounds_returnsOnlyEventsWhollyWithinTheWindow() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);
    IEvent within = savedScheduledEvent(base.plusHours(2), base.plusHours(3));
    savedScheduledEvent(base.plusMinutes(30), base.plusMinutes(45)); // starts before window
    savedScheduledEvent(base.plusHours(5), base.plusHours(6)); // ends after window

    List<IEvent> results = service.getEventsByDateTime(base.plusHours(1), base.plusHours(4));

    assertEquals(List.of(within), results);
  }

  @Test
  void getEventsByDateTime_startBoundIsInclusive() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);
    IEvent onBoundary = savedScheduledEvent(base.plusHours(2), base.plusHours(3));

    List<IEvent> results = service.getEventsByDateTime(base.plusHours(2), null);

    assertEquals(List.of(onBoundary), results);
  }

  @Test
  void getEventsByDateTime_endBoundIsInclusive() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);
    IEvent onBoundary = savedScheduledEvent(base.plusHours(2), base.plusHours(3));

    List<IEvent> results = service.getEventsByDateTime(null, base.plusHours(3));

    assertEquals(List.of(onBoundary), results);
  }

  @Test
  void getEventsByDateTime_whenNoEventsSaved_returnsEmptyList() {
    LocalDateTime base = LocalDateTime.now().plusDays(1);

    assertTrue(service.getEventsByDateTime(base, base.plusHours(1)).isEmpty());
  }

  // --- helpers ---

  private IEvent savedEvent() {
    IEvent event = new Event("Original Title", EventCategory.MUSIC);
    repository.save(event);
    return event;
  }

  private IEvent savedEvent(EventCategory category) {
    IEvent event = new Event("Original Title", category);
    repository.save(event);
    return event;
  }

  private IEvent savedEventWithCapacity(int capacity) {
    IEvent event = new Event("Original Title", capacity, EventCategory.MUSIC);
    repository.save(event);
    return event;
  }

  private IEvent savedScheduledEvent(LocalDateTime start, LocalDateTime end) {
    IEvent event = new Event("Original Title", EventCategory.MUSIC);
    event.setLocation("Central Hall");
    event.setDateTime(start, end);
    repository.save(event);
    return event;
  }
}
