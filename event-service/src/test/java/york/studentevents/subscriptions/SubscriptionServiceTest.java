package york.studentevents.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static york.studentevents.subscriptions.ISubscription.SubscriptionSource.EXPLICIT;
import static york.studentevents.subscriptions.ISubscription.SubscriptionSource.REGISTRATION;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;

/** Tests {@link SubscriptionService} against a real in-memory subscription repository. */
class SubscriptionServiceTest {

  private InMemorySubscriptionRepository repository;
  private SubscriptionService service;

  private UUID userA;
  private UUID userB;
  private UUID eventX;
  private UUID eventY;

  // Captures what UserEventObserver logs, so publish() delivery can be asserted.
  private Logger observerLogger;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    repository = new InMemorySubscriptionRepository();
    service = new SubscriptionService(repository);

    userA = UUID.randomUUID();
    userB = UUID.randomUUID();
    eventX = UUID.randomUUID();
    eventY = UUID.randomUUID();

    observerLogger = (Logger) LoggerFactory.getLogger(UserEventObserver.class);
    observerLogger.setLevel(Level.INFO);
    logAppender = new ListAppender<>();
    logAppender.start();
    observerLogger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    observerLogger.detachAppender(logAppender);
  }

  private List<String> loggedMessages() {
    return logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }

  // --- subscribe: REGISTRATION ---

  @Test
  void subscribe_registration_whenNotSubscribed_createsSubscription() {
    service.subscribe(userA, eventX, REGISTRATION);

    assertTrue(service.isSubscribed(userA, eventX));
    assertEquals(REGISTRATION, repository.findByID(userA, eventX).orElseThrow().getSource());
  }

  @Test
  void subscribe_registration_whenAlreadyRegistered_isIdempotent() {
    service.subscribe(userA, eventX, REGISTRATION);
    service.subscribe(userA, eventX, REGISTRATION);

    assertTrue(service.isSubscribed(userA, eventX));
    assertEquals(1, repository.findAllByEventId(eventX).size());
  }

  @Test
  void subscribe_registration_whenExplicitlySubscribed_doesNotOverwrite() {
    service.subscribe(userA, eventX, EXPLICIT);
    service.subscribe(userA, eventX, REGISTRATION);

    assertEquals(1, repository.findAllByEventId(eventX).size());
    assertEquals(EXPLICIT, repository.findByID(userA, eventX).orElseThrow().getSource());
  }

  // --- subscribe: EXPLICIT ---

  @Test
  void subscribe_explicit_whenNotSubscribed_createsExplicitSubscription() {
    service.subscribe(userA, eventX, EXPLICIT);

    assertTrue(service.isSubscribed(userA, eventX));
    assertEquals(EXPLICIT, repository.findByID(userA, eventX).orElseThrow().getSource());
  }

  @Test
  void subscribe_explicit_whenAlreadyExplicit_throws() {
    service.subscribe(userA, eventX, EXPLICIT);

    assertThrows(IllegalArgumentException.class,
        () -> service.subscribe(userA, eventX, EXPLICIT));
  }

  @Test
  void subscribe_explicit_whenRegistered_upgradesToStickyExplicit() {
    service.subscribe(userA, eventX, REGISTRATION);
    service.subscribe(userA, eventX, EXPLICIT);

    assertEquals(1, repository.findAllByEventId(eventX).size());
    assertEquals(EXPLICIT, repository.findByID(userA, eventX).orElseThrow().getSource());
  }

  // --- unsubscribe: REGISTRATION (deregistration) ---

  @Test
  void unsubscribe_registration_removesRegistrationSubscription() {
    service.subscribe(userA, eventX, REGISTRATION);
    service.unsubscribe(userA, eventX, REGISTRATION);

    assertFalse(service.isSubscribed(userA, eventX));
  }

  @Test
  void unsubscribe_registration_leavesExplicitSubscriptionUntouched() {
    service.subscribe(userA, eventX, EXPLICIT);
    service.unsubscribe(userA, eventX, REGISTRATION);

    assertTrue(service.isSubscribed(userA, eventX));
    assertEquals(EXPLICIT, repository.findByID(userA, eventX).orElseThrow().getSource());
  }

  @Test
  void unsubscribe_registration_whenNotSubscribed_isNoOp() {
    service.unsubscribe(userA, eventX, REGISTRATION);

    assertFalse(service.isSubscribed(userA, eventX));
  }

  // --- unsubscribe: EXPLICIT ---

  @Test
  void unsubscribe_explicit_removesExplicitSubscription() {
    service.subscribe(userA, eventX, EXPLICIT);
    service.unsubscribe(userA, eventX, EXPLICIT);

    assertFalse(service.isSubscribed(userA, eventX));
  }

  @Test
  void unsubscribe_explicit_removesRegistrationSubscription() {
    service.subscribe(userA, eventX, REGISTRATION);
    service.unsubscribe(userA, eventX, EXPLICIT);

    assertFalse(service.isSubscribed(userA, eventX));
  }

  @Test
  void unsubscribe_explicit_whenNotSubscribed_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> service.unsubscribe(userA, eventX, EXPLICIT));
  }

  // --- isSubscribed ---

  @Test
  void isSubscribed_whenNotSubscribed_returnsFalse() {
    assertFalse(service.isSubscribed(userA, eventX));
  }

  @Test
  void isSubscribed_isScopedToUserAndEvent() {
    service.subscribe(userA, eventX, REGISTRATION);

    assertTrue(service.isSubscribed(userA, eventX));
    assertFalse(service.isSubscribed(userB, eventX));
    assertFalse(service.isSubscribed(userA, eventY));
  }

  // --- publish ---

  @Test
  void publish_notifiesEverySubscriberOfEvent() {
    service.subscribe(userA, eventX, REGISTRATION);
    service.subscribe(userB, eventX, EXPLICIT);

    service.publish(eventX, NotificationType.CAPACITY_WARNING);

    List<String> messages = loggedMessages();
    assertEquals(2, messages.size());
    assertTrue(messages.stream().anyMatch(m -> m.contains(userA.toString())));
    assertTrue(messages.stream().anyMatch(m -> m.contains(userB.toString())));
  }

  @Test
  void publish_onlyNotifiesSubscribersOfTheTargetEvent() {
    service.subscribe(userA, eventX, REGISTRATION);
    service.subscribe(userB, eventY, REGISTRATION);

    service.publish(eventX, NotificationType.CAPACITY_WARNING);

    List<String> messages = loggedMessages();
    assertEquals(1, messages.size());
    assertTrue(messages.get(0).contains(userA.toString()));
    assertFalse(messages.get(0).contains(userB.toString()));
  }

  @Test
  void publish_includesTheReasonAndEvent() {
    service.subscribe(userA, eventX, REGISTRATION);

    service.publish(eventX, NotificationType.DATE_CHANGE);

    List<String> messages = loggedMessages();
    assertEquals(1, messages.size());
    assertTrue(messages.get(0).contains("DATE_CHANGE"));
    assertTrue(messages.get(0).contains(eventX.toString()));
  }

  @Test
  void publish_withNoSubscribers_isNoOpAndDoesNotThrow() {
    service.publish(eventX, NotificationType.CAPACITY_WARNING);

    assertTrue(loggedMessages().isEmpty());
  }

  // --- sticky-explicit lifecycle, end to end ---

  @Test
  void lifecycle_explicitSubscriptionSurvivesRegisterAndDeregister() {
    service.subscribe(userA, eventX, EXPLICIT);
    service.subscribe(userA, eventX, REGISTRATION);    // no-op: explicit is sticky
    service.unsubscribe(userA, eventX, REGISTRATION);  // deregister leaves explicit intact

    assertTrue(service.isSubscribed(userA, eventX));

    service.unsubscribe(userA, eventX, EXPLICIT);       // only explicit unsubscribe removes it
    assertFalse(service.isSubscribed(userA, eventX));
  }
}