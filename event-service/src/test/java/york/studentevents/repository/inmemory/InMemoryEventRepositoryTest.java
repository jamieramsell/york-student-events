package york.studentevents.repository.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.events.IEvent;
import york.studentevents.events.Event;

class InMemoryEventRepositoryTest {

  private InMemoryEventRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventRepository();
  }

  // --- Initial state ---

  @Test
  void findAll_onNewRepository_returnsEmptyList() {
    assertTrue(repository.findAll().isEmpty());
  }

  @Test
  void findAll_onNewRepository_isNotNull() {
    assertNotNull(repository.findAll());
  }

  // --- save: single event ---

  @Test
  void save_singleEvent_appearsInFindAll() {
    IEvent event = new Event("York Ball", 200, "Social");
    repository.save(event);

    List<IEvent> result = repository.findAll();
    assertEquals(1, result.size());
  }

  @Test
  void save_singleEvent_correctIdPreserved() {
    Event event = new Event("York Ball", 200, "Social");
    repository.save(event);

    UUID storedId = repository.findAll().get(0).getId();
    assertEquals(event.getId(), storedId);
  }

  @Test
  void save_singleEventWithNoCapacity_appearsInFindAll() {
    Event event = new Event("Film Night", "Culture");
    repository.save(event);

    assertEquals(1, repository.findAll().size());
  }

  // --- save: multiple events ---

  @Test
  void save_twoDistinctEvents_returnsCountOfTwo() {
    repository.save(new Event("Quiz Night", 50, "Social"));
    repository.save(new Event("Band Night", 150, "Music"));

    assertEquals(2, repository.findAll().size());
  }

  @Test
  void save_threeDistinctEvents_allIdsReturnedByFindAll() {
    Event e1 = new Event("Quiz Night", 50, "Social");
    Event e2 = new Event("Band Night", 150, "Music");
    Event e3 = new Event("5-a-side", "Sport");
    repository.save(e1);
    repository.save(e2);
    repository.save(e3);

    List<UUID> storedIds = repository.findAll().stream().map(IEvent::getId).toList();
    assertTrue(storedIds.contains(e1.getId()));
    assertTrue(storedIds.contains(e2.getId()));
    assertTrue(storedIds.contains(e3.getId()));
  }

  @Test
  void save_tenEvents_allStoredAndRetrievable() {
    int count = 10;
    for (int i = 0; i < count; i++) {
      repository.save(new Event("Event " + i, 30, "Social"));
    }

    assertEquals(count, repository.findAll().size());
  }

  // --- save: overwrite on same ID ---

  @Test
  void save_sameEventTwice_doesNotCreateDuplicate() {
    Event event = new Event("Open Mic", 60, "Music");
    repository.save(event);
    repository.save(event);

    assertEquals(1, repository.findAll().size());
  }

  @Test
  void save_sameEventAgainAfterMutation_overwritesEntry() {
    Event event = new Event("Hackathon", 100, "Academic");
    repository.save(event);

    event.setTitle("Hackathon 2026");
    repository.save(event);

    List<IEvent> result = repository.findAll();
    assertEquals(1, result.size());
    assertEquals("Hackathon 2026", result.get(0).getTitle());
  }

  // --- findAll: defensive copy ---

  @Test
  void findAll_mutatingReturnedList_doesNotAffectRepository() {
    repository.save(new Event("Debate Club", 40, "Academic"));

    List<IEvent> firstResult = repository.findAll();
    firstResult.add(new Event("Phantom Event", "Sport"));

    assertEquals(1, repository.findAll().size());
  }

  // --- Reference semantics ---

  @Test
  void save_mutatingEventAfterSave_isReflectedInFindAll() {
    Event event = new Event("Original Title", 80, "Social");
    repository.save(event);

    event.setTitle("Updated Title");

    assertEquals("Updated Title", repository.findAll().get(0).getTitle());
  }

  // --- Interleaved save and findAll ---

  @Test
  void findAll_calledBetweenSaves_returnsOnlyEventsSavedSoFar() {
    Event e1 = new Event("First Event", 30, "Music");
    repository.save(e1);
    assertEquals(1, repository.findAll().size());

    Event e2 = new Event("Second Event", 60, "Sport");
    repository.save(e2);
    assertEquals(2, repository.findAll().size());
  }

}
