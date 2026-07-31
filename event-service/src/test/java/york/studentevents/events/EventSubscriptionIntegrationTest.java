package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.subscriptions.SubscriptionService;
import york.studentevents.users.Host;
import york.studentevents.users.Student;

/**
 * End-to-end integration test for the event-subscription feature.
 *
 * <p>Unlike {@code StudentEventServiceTest}, which isolates {@code StudentEventService} from
 * {@code SubscriptionService} with a recording double, every component here is real: the user,
 * event, and subscription repositories; {@code SubscriptionService}; and
 * {@code StudentEventService} itself. Notification delivery is verified by capturing what
 * {@code UserEventObserver} logs, since that is currently the only delivery channel.
 */
class EventSubscriptionIntegrationTest {

  private InMemoryUserRepository userRepository;
  private InMemoryEventRepository eventRepository;
  private SubscriptionService subscriptionService;
  private EventService eventService;
  private StudentEventService service;

  private Logger observerLogger;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    userRepository = new InMemoryUserRepository();
    eventRepository = new InMemoryEventRepository();
    subscriptionService = new SubscriptionService(new InMemorySubscriptionRepository());
    eventService = new EventService(eventRepository);
    service = new StudentEventService(
        eventRepository, userRepository, subscriptionService, eventService
    );

    observerLogger =
        (Logger) LoggerFactory.getLogger(york.studentevents.subscriptions.UserEventObserver.class);
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

  // --- Scenario 1: register -> subscribe -> persists ---

  @Test
  void registering_persistsRealRegistrationSubscription() {
    Student student = newStudent();
    Event event = newEvent(5);

    service.registerForEvent(student.getId(), event.getId());

    assertTrue(subscriptionService.isSubscribed(student.getId(), event.getId()));
    assertTrue(service.getEventsForStudent(student.getId()).contains(event));
  }

  // --- Scenario 2: capacity threshold -> real publish -> real delivery ---

  @Test
  void reachingCapacityThreshold_notifiesExactlyTheEventsRealSubscribers() {
    Event target = newEvent(5); // threshold = ceil(5 * 0.8) = 4
    Event other = newEvent(5);

    // A student subscribed to a different event must never appear in target's notification.
    Student bystander = newStudent();
    service.registerForEvent(bystander.getId(), other.getId());

    List<Student> subscribers = List.of(newStudent(), newStudent(), newStudent(), newStudent());
    subscribers.forEach(student -> service.registerForEvent(student.getId(), target.getId()));

    List<String> messages = loggedMessages();
    assertEquals(4, messages.size());
    assertTrue(messages.stream().allMatch(m -> m.contains(target.getId().toString())));
    assertTrue(messages.stream().allMatch(m -> m.contains("CAPACITY_WARNING")));
    subscribers.forEach(
        student -> assertTrue(
            messages.stream()
            .anyMatch(m -> m.contains(
                student.getId()
                .toString()
            ))
        )
    );
    assertFalse(messages.stream().anyMatch(m -> m.contains(bystander.getId().toString())));
  }

  // --- Scenario 3: deregister -> unsubscribe -> excluded from future notifications ---

  @Test
  void deregistering_removesTheSubscriptionSoFutureNotificationsExcludeThem() {
    Event event = newEvent(5); // threshold = 4

    Student leaving = newStudent();
    List<Student> remaining = List.of(newStudent(), newStudent(), newStudent());

    service.registerForEvent(leaving.getId(), event.getId());
    remaining.forEach(student -> service.registerForEvent(student.getId(), event.getId()));

    // The 4th registration above already crossed the threshold once; clear that and re-cross it.
    logAppender.list.clear();

    service.deregisterFromEvent(leaving.getId(), event.getId());
    assertFalse(subscriptionService.isSubscribed(leaving.getId(), event.getId()));

    Student replacement = newStudent();
    service.registerForEvent(replacement.getId(), event.getId()); // re-crosses the threshold 3 -> 4

    List<String> messages = loggedMessages();
    assertEquals(4, messages.size());
    assertFalse(messages.stream().anyMatch(m -> m.contains(leaving.getId().toString())));
    assertTrue(messages.stream().anyMatch(m -> m.contains(replacement.getId().toString())));
  }

  // --- Scenario 4: explicit subscription is sticky across register/deregister ---

  @Test
  void explicitSubscription_survivesRegisterAndDeregister_untilExplicitlyRemoved() {
    Student student = newStudent();
    Event event = newEvent(5);

    service.subscribeToEvent(student.getId(), event.getId());

    service.registerForEvent(student.getId(), event.getId());
    assertTrue(subscriptionService.isSubscribed(student.getId(), event.getId()));

    service.deregisterFromEvent(student.getId(), event.getId());
    assertTrue(subscriptionService.isSubscribed(student.getId(), event.getId()));
    assertFalse(service.getEventsForStudent(student.getId()).contains(event));

    service.unsubscribeFromEvent(student.getId(), event.getId());
    assertFalse(subscriptionService.isSubscribed(student.getId(), event.getId()));
  }

  // --- Scenario 5: capacity exceeded -> rejected before any subscription is created ---

  @Test
  void registrationRejectedAtCapacity_neverCreatesSubscription() {
    Event event = newEvent(3);
    List.of(newStudent(), newStudent(), newStudent())
        .forEach(student -> service.registerForEvent(student.getId(), event.getId()));

    Student rejected = newStudent();
    assertThrows(CapacityExceededException.class,
        () -> service.registerForEvent(rejected.getId(), event.getId()));

    assertFalse(subscriptionService.isSubscribed(rejected.getId(), event.getId()));
    assertTrue(service.getEventsForStudent(rejected.getId()).isEmpty());
  }

  // --- Scenario 6: a Host cannot enter the subscription flow at all ---

  @Test
  void hostCannotRegisterOrBeSubscribed() {
    Host host = new Host("host", "host@york.ac.uk", new HashSet<>());
    userRepository.save(host);
    Event event = newEvent(5);

    assertThrows(york.studentevents.exceptions.UserNotAuthorisedException.class,
        () -> service.registerForEvent(host.getId(), event.getId()));
    assertFalse(subscriptionService.isSubscribed(host.getId(), event.getId()));
  }

  // --- helpers ---

  private Student newStudent() {
    Student student =
        new Student("student", "student@york.ac.uk", UUID.randomUUID(), new HashSet<>());
    userRepository.save(student);
    return student;
  }

  private Event newEvent(int capacity) {
    Event event = new Event("Event", capacity, york.studentevents.events.EventCategory.MUSIC);
    eventRepository.save(event);
    return event;
  }
}