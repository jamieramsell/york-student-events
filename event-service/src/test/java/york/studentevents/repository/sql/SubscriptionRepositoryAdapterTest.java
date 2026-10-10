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
import org.springframework.data.domain.Page;
import york.studentevents.events.Event;
import york.studentevents.events.EventCategory;
import york.studentevents.subscriptions.ISubscription;
import york.studentevents.subscriptions.ISubscriptionRepository;
import york.studentevents.subscriptions.Subscription;
import york.studentevents.users.IStudent;
import york.studentevents.users.UserService;

/**
 * Integration tests for {@link SubscriptionRepositoryAdapter}, exercising it against a real Spring
 * Data JPA layer backed by an in-memory H2 database via
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}.
 *
 * <p>Each test constructs the adapter around the injected {@link JpaSubscriptionRepository} proxy,
 *     so the assertions cover both the CRUD delegation and the adapter's own translation behaviour:
 *     the null-guards on {@code save}, {@code delete}, {@code findByID}, {@code findAllByEventId}
 *     and {@code findAllByUserId}, the mapping of a missing row to
 *     {@link java.util.Optional#empty()}, the {@link java.util.NoSuchElementException} raised when
 *     deleting a non-existent Subscription, and the overwrite-on-save semantics of an existing ID.
 *
 * <p>Every persisted subscription references a user and an event through the
 * {@code student_event_subscriptions} foreign keys, so {@link #setUp()} seeds one {@link IStudent}
 * and one {@link Event} and exposes their IDs as {@link #studentId} / {@link #eventId} for the
 * tests to build subscriptions from.
 *
 * @see SubscriptionRepositoryAdapter
 * @see JpaSubscriptionRepository
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class SubscriptionRepositoryAdapterTest {

  @Autowired
  private JpaSubscriptionRepository jpa;

  @Autowired
  private JpaUserRepository userJpa;

  @Autowired
  private JpaEventRepository eventJpa;

  private ISubscriptionRepository subscriptionRepository;

  private UserService userService;

  private UUID studentId;

  private UUID eventId;

  @BeforeEach
  void setUp() {
    subscriptionRepository = new SubscriptionRepositoryAdapter(jpa);
    userService = new UserService(new UserRepositoryAdapter(userJpa));

    // Seed the foreign-key targets (users.id, event.id) that every persisted subscription
    // references, so the student_event_subscriptions inserts are legal.
    IStudent student = userService.createStudent("jamieramsell", "jamie@york.ac.uk", "password");
    Event event = new Event("Test Event", EventCategory.SOCIAL);
    eventJpa.saveAndFlush(event);

    studentId = student.getId();
    eventId = event.getId();
  }

  /**
   * Persists a fresh student and returns its ID, for tests that need a second FK-valid user.
   *
   * @param username the new student's username
   * @param email the new student's email address
   * @return the persisted student's ID
   */
  private UUID persistStudentId(String username, String email) {
    return userService.createStudent(username, email, "password").getId();
  }

  /**
   * Persists a fresh event and returns its ID, for tests that need a second FK-valid event.
   *
   * @param title the new event's title
   * @return the persisted event's ID
   */
  private UUID persistEventId(String title) {
    Event event = new Event(title, EventCategory.SOCIAL);
    eventJpa.saveAndFlush(event);
    return event.getId();
  }

  @Test
  void save_RejectsNullSubscription() {
    ISubscription nullSub = null;
    assertThrows(IllegalArgumentException.class, () -> subscriptionRepository.save(nullSub));
  }

  @Test
  void save_PersistsToJpa() {
    ISubscription sub = new Subscription(
        studentId,
        eventId,
        ISubscription.SubscriptionSource.EXPLICIT
    );
    subscriptionRepository.save(sub);
    assertTrue(jpa.existsById(sub.getId()));
  }

  @Test
  void save_WithExistingId_Overwrites() {
    ISubscription sub = new Subscription(
        studentId,
        eventId,
        ISubscription.SubscriptionSource.EXPLICIT
    );
    subscriptionRepository.save(sub);
    subscriptionRepository.save(sub);

    // Use JPA here to avoid relying on a separate adapter method
    assertEquals(1, jpa.findAll().size());
  }

  @Test
  void delete_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> subscriptionRepository.delete(nullId));
  }

  @Test
  void delete_RejectsNonexistentSubscription() {
    UUID fakeId = UUID.randomUUID();
    assertThrows(NoSuchElementException.class, () -> subscriptionRepository.delete(fakeId));
  }

  @Test
  void delete_RemovesSubscriptionFromJpa() {
    Subscription sub = new Subscription(
        studentId,
        eventId,
        ISubscription.SubscriptionSource.EXPLICIT
    );
    jpa.saveAndFlush(sub); // Use JPA here to avoid relying on a separate adapter method

    subscriptionRepository.delete(sub.getId());
    assertFalse(jpa.existsById(sub.getId()));
  }

  @Test
  void findById_RejectsNullId() {
    UUID nullId = null;
    assertThrows(IllegalArgumentException.class, () -> subscriptionRepository.findByID(nullId));
  }

  @Test
  void findById_ReturnsEmptyOptional_OnNonexistentSubscription() {
    UUID fakeId = UUID.randomUUID();
    Optional<ISubscription> emptyOptional = Optional.empty();
    assertEquals(emptyOptional, subscriptionRepository.findByID(fakeId));
  }

  @Test
  void findById_RetrievesSubscriptionFromJpa() {
    Subscription sub = new Subscription(
        studentId,
        eventId,
        ISubscription.SubscriptionSource.EXPLICIT
    );
    jpa.saveAndFlush(sub); // Use JPA here to avoid relying on a separate adapter method

    ISubscription retrievedSub = subscriptionRepository.findByID(sub.getId()).get();
    assertEquals(sub.getId(), retrievedSub.getId());
    assertEquals(sub.getUserId(), retrievedSub.getUserId());
    assertEquals(sub.getEventId(), retrievedSub.getEventId());
  }

  @Test
  void findAll_RetrievesAllSubscriptions() {
    // Distinct events so each (user, event) pair is unique. Keeps the test valid if a unique
    // constraint on (user_id, event_id) is added later.
    List<Subscription> subList = new ArrayList<>();
    subList.add(
        new Subscription(
            studentId,
            persistEventId("Event 1"),
            ISubscription.SubscriptionSource.REGISTRATION
        ));
    subList.add(
        new Subscription(
            studentId,
            persistEventId("Event 2"),
            ISubscription.SubscriptionSource.REGISTRATION
        ));
    subList.add(
        new Subscription(
            studentId,
            persistEventId("Event 3"),
            ISubscription.SubscriptionSource.REGISTRATION
        ));
    jpa.saveAllAndFlush(subList); // Use JPA here to avoid relying on a separate adapter method

    Page<ISubscription> savedSubs = subscriptionRepository.findAll(0, 100);
    assertEquals(3, savedSubs.getTotalElements());
  }

  @Test
  void findAll_ReturnsEmptyPage_ForEmptyRepository() {
    assertEquals(0, subscriptionRepository.findAll(0, 100).getTotalElements());
  }

  // --- findByID(userId, eventId) ---

  @Test
  void findByUserAndEvent_RejectsNullUserId() {
    assertThrows(
        IllegalArgumentException.class, () -> subscriptionRepository.findByID(null, eventId));
  }

  @Test
  void findByUserAndEvent_RejectsNullEventId() {
    assertThrows(
        IllegalArgumentException.class, () -> subscriptionRepository.findByID(studentId, null));
  }

  @Test
  void findByUserAndEvent_RetrievesMatchingSubscription() {
    Subscription sub = new Subscription(
        studentId,
        eventId,
        ISubscription.SubscriptionSource.EXPLICIT
    );
    jpa.saveAndFlush(sub); // Use JPA here to avoid relying on a separate adapter method

    Optional<ISubscription> retrieved = subscriptionRepository.findByID(studentId, eventId);

    assertTrue(retrieved.isPresent());
    assertEquals(sub.getId(), retrieved.get().getId());
  }

  @Test
  void findByUserAndEvent_ReturnsEmptyOptional_WhenNoSubscriptionLinksThem() {
    // The student and event both exist (seeded in setUp) but no subscription connects them.
    assertEquals(Optional.empty(), subscriptionRepository.findByID(studentId, eventId));
  }

  // --- findAllByUserId ---

  @Test
  void findAllByUserId_RejectsNullUserId() {
    assertThrows(
        IllegalArgumentException.class, () -> subscriptionRepository.findAllByUserId(null, 0, 100));
  }

  @Test
  void findAllByUserId_ReturnsOnlyThatUsersSubscriptions() {
    UUID otherEventId = persistEventId("Other Event");
    UUID otherStudentId = persistStudentId("otherstudent", "other@york.ac.uk");

    // Two subscriptions for the seeded student (distinct events), one for a different student.
    jpa.saveAndFlush(
        new Subscription(studentId, eventId, ISubscription.SubscriptionSource.EXPLICIT));
    jpa.saveAndFlush(
        new Subscription(studentId, otherEventId, ISubscription.SubscriptionSource.EXPLICIT));
    jpa.saveAndFlush(
        new Subscription(otherStudentId, eventId, ISubscription.SubscriptionSource.EXPLICIT));

    List<ISubscription> result = subscriptionRepository.findAllByUserId(studentId, 0, 100).getContent();

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(sub -> sub.getUserId().equals(studentId)));
  }

  @Test
  void findAllByUserId_ReturnsEmptyPage_WhenUserHasNoSubscriptions() {
    // The student exists but has no subscriptions.
    assertEquals(0, subscriptionRepository.findAllByUserId(studentId, 0, 100).getTotalElements());
  }

  // --- findAllByEventId ---

  @Test
  void findAllByEventId_RejectsNullEventId() {
    assertThrows(
        IllegalArgumentException.class, () -> subscriptionRepository.findAllByEventId(null, 0, 100));
  }

  @Test
  void findAllByEventId_ReturnsOnlyThatEventsSubscriptions() {
    UUID otherEventId = persistEventId("Other Event");
    UUID otherStudentId = persistStudentId("otherstudent", "other@york.ac.uk");

    // Two subscriptions to the seeded event (distinct users), one to a different event.
    jpa.saveAndFlush(
        new Subscription(studentId, eventId, ISubscription.SubscriptionSource.EXPLICIT));
    jpa.saveAndFlush(
        new Subscription(otherStudentId, eventId, ISubscription.SubscriptionSource.EXPLICIT));
    jpa.saveAndFlush(
        new Subscription(studentId, otherEventId, ISubscription.SubscriptionSource.EXPLICIT));

    List<ISubscription> result = subscriptionRepository.findAllByEventId(eventId, 0, 100).getContent();

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(sub -> sub.getEventId().equals(eventId)));
  }

  @Test
  void findAllByEventId_ReturnsEmptyPage_WhenEventHasNoSubscriptions() {
    // The event exists but has no subscriptions.
    assertEquals(0, subscriptionRepository.findAllByEventId(eventId, 0, 100).getTotalElements());
  }
}
