package york.studentevents.repository.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static york.studentevents.subscriptions.ISubscription.SubscriptionSource.REGISTRATION;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.subscriptions.Subscription;

/** Tests the specialised query methods of {@link InMemorySubscriptionRepository}. */
class InMemorySubscriptionRepositoryTest {

  private InMemorySubscriptionRepository repository;
  private UUID userA;
  private UUID userB;
  private UUID eventX;
  private UUID eventY;

  @BeforeEach
  void setUp() {
    repository = new InMemorySubscriptionRepository();
    userA = UUID.randomUUID();
    userB = UUID.randomUUID();
    eventX = UUID.randomUUID();
    eventY = UUID.randomUUID();
  }

  @Test
  void findById_whenSubscriptionExists_returnsIt() {
    Subscription subscription = new Subscription(userA, eventX, REGISTRATION);
    repository.save(subscription);

    assertTrue(repository.findByID(userA, eventX).isPresent());
    assertEquals(subscription.getId(), repository.findByID(userA, eventX).get().getId());
  }

  @Test
  void findById_whenNoSubscriptionExists_returnsEmpty() {
    assertTrue(repository.findByID(userA, eventX).isEmpty());
  }

  @Test
  void findById_whenMultipleRecordsForSameUserAndEvent_throws() {
    repository.save(new Subscription(userA, eventX, REGISTRATION));
    repository.save(new Subscription(userA, eventX, REGISTRATION));

    assertThrows(IllegalStateException.class, () -> repository.findByID(userA, eventX));
  }

  @Test
  void findAllByUserId_returnsOnlyThatUsersSubscriptions() {
    repository.save(new Subscription(userA, eventX, REGISTRATION));
    repository.save(new Subscription(userA, eventY, REGISTRATION));
    repository.save(new Subscription(userB, eventX, REGISTRATION));

    assertEquals(2, repository.findAllByUserId(userA).size());
    assertTrue(repository.findAllByUserId(userA).stream()
        .allMatch(sub -> sub.getUserId().equals(userA)));
  }

  @Test
  void findAllByEventId_returnsOnlyThatEventsSubscriptions() {
    repository.save(new Subscription(userA, eventX, REGISTRATION));
    repository.save(new Subscription(userB, eventX, REGISTRATION));
    repository.save(new Subscription(userA, eventY, REGISTRATION));

    assertEquals(2, repository.findAllByEventId(eventX).size());
    assertTrue(repository.findAllByEventId(eventX).stream()
        .allMatch(sub -> sub.getEventId().equals(eventX)));
  }

  @Test
  void delete_removesTheSubscription() {
    Subscription subscription = new Subscription(userA, eventX, REGISTRATION);
    repository.save(subscription);

    repository.delete(subscription.getId());

    assertFalse(repository.findByID(userA, eventX).isPresent());
  }
}