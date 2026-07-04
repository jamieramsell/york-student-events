package york.studentevents.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests {@link EventNotificationService}, the per-event observable in the Observer pattern. */
class EventNotificationServiceTest {

  private UUID eventId;
  private EventNotificationService service;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
    service = new EventNotificationService(eventId);
  }

  @Test
  void getEventId_returnsTheEventTheObservableIsBoundTo() {
    assertEquals(eventId, service.getEventId());
  }

  @Test
  void attach_whenObserverIsNew_returnsTrue() {
    assertTrue(service.attach(new RecordingObserver()));
  }

  @Test
  void attach_sameUserTwice_returnsFalseAndDoesNotDuplicate() {
    UUID userId = UUID.randomUUID();
    assertTrue(service.attach(new UserEventObserver(userId)));
    assertFalse(service.attach(new UserEventObserver(userId)));
  }

  @Test
  void detach_byEquivalentObserver_removesTheOriginal() {
    UUID userId = UUID.randomUUID();
    service.attach(new UserEventObserver(userId));

    // A different instance representing the same user still detaches the original.
    assertTrue(service.detach(new UserEventObserver(userId)));
  }

  @Test
  void detach_whenNotAttached_returnsFalse() {
    assertFalse(service.detach(new UserEventObserver(UUID.randomUUID())));
  }

  @Test
  void notifyObservers_notifiesEachAttachedObserverWithSourceAndReason() {
    RecordingObserver first = new RecordingObserver();
    RecordingObserver second = new RecordingObserver();
    service.attach(first);
    service.attach(second);

    service.notifyObservers(NotificationType.CAPACITY_WARNING);

    assertEquals(1, first.updateCount);
    assertEquals(1, second.updateCount);
    assertSame(service, first.lastSource);
    assertEquals(NotificationType.CAPACITY_WARNING, first.lastReason);
  }

  @Test
  void notifyObservers_doesNotNotifyDetachedObservers() {
    RecordingObserver observer = new RecordingObserver();
    service.attach(observer);
    service.detach(observer);

    service.notifyObservers(NotificationType.DATE_CHANGE);

    assertEquals(0, observer.updateCount);
  }

  @Test
  void notifyObservers_withNoObservers_doesNotThrow() {
    service.notifyObservers(NotificationType.CAPACITY_WARNING);
  }

  /** A test double that records the notifications it receives. */
  private static final class RecordingObserver implements IObserver {
    private int updateCount;
    private IObservable lastSource;
    private NotificationType lastReason;

    @Override
    public void update(IObservable source, NotificationType reason) {
      updateCount++;
      lastSource = source;
      lastReason = reason;
    }
  }
}