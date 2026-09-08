package york.studentevents.repository.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import york.studentevents.events.Event;
import york.studentevents.events.EventCategory;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;

/**
 * Integration tests for {@link EventRepositoryAdapter}, exercising it against a real Spring Data
 * JPA layer backed by an in-memory H2 database via
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}.
 *
 * <p>Each test constructs the adapter around the injected {@link JpaEventRepository} proxy, so the
 *     assertions cover both the CRUD delegation and the adapter's own translation behaviour: the
 *     null-guards on {@code save}, {@code delete} and {@code findByID}, the mapping of a missing
 *     row to {@link java.util.Optional#empty()}, the {@link java.util.NoSuchElementException}
 *     raised when deleting a non-existent event, and the overwrite-on-save semantics of an existing
 *     ID.
 *
 * @see EventRepositoryAdapter
 * @see JpaEventRepository
 */
@DataJpaTest 
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class EventRepositoryAdapterTest {
  
  @Autowired
  private JpaEventRepository jpa;

  private IEventRepository eventRepository;

  @BeforeEach
  void setUp() {
    eventRepository = new EventRepositoryAdapter(jpa);
  }

  @Test
  void save_RejectsNullEvent() {
    IEvent nullEvent = null;
    assertThrows(IllegalArgumentException.class, () -> eventRepository.save(nullEvent));
  }

  @Test
  void save_PersistsToJpa() {
    IEvent event = new Event("Test Title", EventCategory.ACADEMIC);
    eventRepository.save(event);
    assertTrue(jpa.existsById(event.getId()));
  }

  @Test 
  void save_WithExistingId_Overwrites() {
    IEvent event = new Event("Test Title 1", EventCategory.ACADEMIC);
    eventRepository.save(event);

    event.setTitle("Test Title 2"); // Event keeps its original ID here but has a new title record
    eventRepository.save(event);
    
    // Use JPA here to avoid relying on a seperate adapter method
    assertEquals(1, jpa.findAll().size()); 

    // Assert that the saved title has been overwritten
    assertEquals(event.getTitle(), jpa.findById(event.getId()).get().getTitle());
  }

  @Test
  void delete_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> eventRepository.delete(nullId));
  }

  @Test
  void delete_RejectsNonexistentEvent() {
    UUID fakeId = UUID.randomUUID();
    assertThrows(NoSuchElementException.class, () -> eventRepository.delete(fakeId));
  }

  @Test 
  void delete_RemovesEventFromJpa() {
    Event event = new Event("Test Title", EventCategory.ACADEMIC);
    jpa.saveAndFlush(event); // Use JPA here to avoid relying on a seperate adapter method

    eventRepository.delete(event.getId());
    assertFalse(jpa.existsById(event.getId()));
  }

  @Test
  void findById_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> eventRepository.findByID(nullId));
  }

  @Test
  void findById_ReturnsEmptyOptional_OnNonexistentEvent() {
    UUID fakeId = UUID.randomUUID();
    Optional<IEvent> emptyOptional = Optional.empty();
    assertEquals(emptyOptional, eventRepository.findByID(fakeId));
  }

  @Test
  void findById_RetrievesEventFromJpa() {
    Event event = new Event("Test Title", EventCategory.ACADEMIC);
    jpa.saveAndFlush(event); // Use JPA here to avoid relying on a seperate adapter method

    IEvent retrievedEvent = eventRepository.findByID(event.getId()).get();
    assertEquals(event.getId(), retrievedEvent.getId());
    assertEquals(event.getTitle(), retrievedEvent.getTitle());
    assertEquals(event.getCategory(), retrievedEvent.getCategory());
  }

  @Test 
  void findAll_RetrievesAllEvents() {
    List<Event> eventList = new ArrayList<>();
    eventList.add(new Event("Title 1", EventCategory.ACADEMIC));
    eventList.add(new Event("Title 2", EventCategory.SPORTS));
    eventList.add(new Event("Title 3", EventCategory.NIGHTLIFE));
    jpa.saveAllAndFlush(eventList); // Use JPA here to avoid relying on a seperate adapter method

    List<IEvent> savedEvents = eventRepository.findAll();
    assertEquals(3, savedEvents.size());
  }

  @Test 
  void findAll_ReturnsEmptyList_ForEmptyRepository() {
    assertTrue(eventRepository.findAll().isEmpty());
  }
}
