package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.repository.inmemory.InMemoryEventRepository;

/**
 * Integration tests for {@link EventService} wired against the real
 * {@link InMemoryEventRepository}.
 *
 * <p>Unlike a mock-based unit test, these tests exercise the full service-to-repository
 * collaboration so that the contract between the two (delegation, ordering independence,
 * defensive copying) is verified end to end.
 *
 * <p>{@link InMemoryEventRepository} seeds a number of hardcoded events in its constructor, so
 * assertions are expressed relative to the {@link #baseline} count captured in {@link #setUp()}
 * rather than against absolute totals. Likewise, events are located by id rather than by list
 * position, since the backing {@code HashMap} provides no ordering guarantee.
 */
class EventServiceTest {

  private InMemoryEventRepository repository;
  private EventService service;
  private int baseline;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventRepository();
    service = new EventService(repository);
    baseline = service.getAllEvents().size();
  }

  /** Returns the event with the given id from {@code events}, or {@code null} if absent. */
  private static IEvent findById(List<IEvent> events, UUID id) {
    return events.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
  }

  // --- Freshly constructed repository ---

  @Test
  void getAllEvents_onFreshRepository_returnsOnlySeededEvents() {
    assertEquals(baseline, service.getAllEvents().size());
  }

  @Test
  void getAllEvents_onFreshRepository_isNotNull() {
    assertNotNull(service.getAllEvents());
  }

  // --- Single event ---

  @Test
  void getAllEvents_afterSavingOneEvent_returnsThatEvent() {
    Event event = new Event("York Ball", 200, "Social");
    repository.save(event);

    List<IEvent> result = service.getAllEvents();

    assertEquals(baseline + 1, result.size());
    assertNotNull(findById(result, event.getId()));
  }

  @Test
  void getAllEvents_afterSavingEventWithNoCapacity_returnsThatEvent() {
    Event event = new Event("Film Night", "Culture");
    repository.save(event);

    IEvent stored = findById(service.getAllEvents(), event.getId());

    assertNotNull(stored);
    assertEquals("Film Night", stored.getTitle());
  }

  // --- Multiple events ---

  @Test
  void getAllEvents_afterSavingMultipleEvents_returnsAllOfThem() {
    Event e1 = new Event("Quiz Night", 50, "Social");
    Event e2 = new Event("Band Night", 150, "Music");
    Event e3 = new Event("5-a-side", "Sport");
    repository.save(e1);
    repository.save(e2);
    repository.save(e3);

    List<UUID> ids = service.getAllEvents().stream().map(IEvent::getId).toList();

    assertEquals(baseline + 3, ids.size());
    assertTrue(ids.contains(e1.getId()));
    assertTrue(ids.contains(e2.getId()));
    assertTrue(ids.contains(e3.getId()));
  }

  @Test
  void getAllEvents_reflectsEventsSavedAfterServiceConstruction() {
    assertEquals(baseline, service.getAllEvents().size());

    repository.save(new Event("Late Addition", 30, "Social"));

    assertEquals(baseline + 1, service.getAllEvents().size());
  }

  // --- Delegation to repository ---

  @Test
  void getAllEvents_overwrittenEvent_isNotDuplicated() {
    Event event = new Event("Hackathon", 100, "Academic");
    repository.save(event);

    event.setTitle("Hackathon 2026");
    repository.save(event);

    List<IEvent> result = service.getAllEvents();
    assertEquals(baseline + 1, result.size());
    assertEquals("Hackathon 2026", findById(result, event.getId()).getTitle());
  }

  @Test
  void getAllEvents_reflectsMutationsToStoredEvents() {
    Event event = new Event("Original Title", 80, "Social");
    repository.save(event);

    event.setTitle("Updated Title");

    assertEquals("Updated Title", findById(service.getAllEvents(), event.getId()).getTitle());
  }

  // --- Defensive copy ---

  @Test
  void getAllEvents_mutatingReturnedList_doesNotAffectSubsequentCalls() {
    repository.save(new Event("Debate Club", 40, "Academic"));

    List<IEvent> firstResult = service.getAllEvents();
    firstResult.add(new Event("Phantom Event", "Sport"));

    assertEquals(baseline + 1, service.getAllEvents().size());
  }

  // --- Repeated invocation ---

  @Test
  void getAllEvents_calledTwice_returnsConsistentResults() {
    repository.save(new Event("Open Mic", 60, "Music"));

    assertEquals(service.getAllEvents().size(), service.getAllEvents().size());
  }

  @Test
  void getAllEvents_calledBetweenSaves_returnsOnlyEventsSavedSoFar() {
    repository.save(new Event("First Event", 30, "Music"));
    assertEquals(baseline + 1, service.getAllEvents().size());

    repository.save(new Event("Second Event", 60, "Sport"));
    assertEquals(baseline + 2, service.getAllEvents().size());
  }

  @Test
  void getAllEvents_afterSavingManyEvents_returnsAllOfThem() {
    int count = 25;
    for (int i = 0; i < count; i++) {
      repository.save(new Event("Event " + i, 30, "Social"));
    }

    List<IEvent> result = service.getAllEvents();
    assertEquals(baseline + count, result.size());
    assertFalse(result.isEmpty());
  }
}
