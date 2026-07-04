package york.studentevents.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Tests {@link UserEventObserver}: its user-keyed identity and its (log-based) delivery. */
class UserEventObserverTest {

  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger(UserEventObserver.class);
    logger.setLevel(Level.INFO);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  @Test
  void equals_sameUser_isTrue() {
    UUID userId = UUID.randomUUID();
    assertEquals(new UserEventObserver(userId), new UserEventObserver(userId));
  }

  @Test
  void equals_differentUsers_isFalse() {
    assertNotEquals(
        new UserEventObserver(UUID.randomUUID()), new UserEventObserver(UUID.randomUUID()));
  }

  @Test
  void equals_nullOrDifferentType_isFalse() {
    UserEventObserver observer = new UserEventObserver(UUID.randomUUID());
    assertNotEquals(observer, null);
    assertNotEquals(observer, "not an observer");
  }

  @Test
  void hashCode_sameUser_isConsistent() {
    UUID userId = UUID.randomUUID();
    assertEquals(
        new UserEventObserver(userId).hashCode(), new UserEventObserver(userId).hashCode());
  }

  @Test
  void update_logsTheUserReasonAndEvent() {
    UUID userId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UserEventObserver observer = new UserEventObserver(userId);

    observer.update(new EventNotificationService(eventId), NotificationType.CAPACITY_WARNING);

    assertEquals(1, appender.list.size());
    String message = appender.list.get(0).getFormattedMessage();
    assertTrue(message.contains(userId.toString()));
    assertTrue(message.contains("CAPACITY_WARNING"));
    assertTrue(message.contains(eventId.toString()));
  }
}