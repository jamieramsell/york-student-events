package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.subscriptions.ISubscription.SubscriptionSource;
import york.studentevents.subscriptions.NotificationType;
import york.studentevents.subscriptions.SubscriptionService;
import york.studentevents.users.Host;
import york.studentevents.users.Student;

/** Tests {@link StudentEventService} against real in-memory repositories and a recording
 * subscription service. */
class StudentEventServiceTest {

  private InMemoryUserRepository userRepository;
  private InMemoryEventRepository eventRepository;
  private RecordingSubscriptionService subscriptionService;
  private StudentEventService service;

  @BeforeEach
  void setUp() {
    userRepository = new InMemoryUserRepository();
    eventRepository = new InMemoryEventRepository();
    subscriptionService = new RecordingSubscriptionService();
    service = new StudentEventService(eventRepository, userRepository, subscriptionService);
  }

  // --- registerForEvent ---

  @Test
  void registerForEvent_addsEventToStudentAndSubscribes() {
    Student student = newStudent();
    Event event = newEvent(5);

    service.registerForEvent(student.getId(), event.getId());

    assertTrue(service.getEventsForStudent(student.getId()).contains(event));
    assertEquals(1, subscriptionService.subscribeCalls.size());
    RecordingSubscriptionService.Call call = subscriptionService.subscribeCalls.get(0);
    assertEquals(student.getId(), call.userId());
    assertEquals(event.getId(), call.eventId());
    assertEquals(SubscriptionSource.REGISTRATION, call.source());
  }

  @Test
  void registerForEvent_whenUserDoesNotExist_throwsUserNotFoundException() {
    Event event = newEvent(5);
    assertThrows(UserNotFoundException.class,
        () -> service.registerForEvent(UUID.randomUUID(), event.getId()));
  }

  @Test
  void registerForEvent_whenUserIsHost_throwsUserNotAuthorisedException() {
    Host host = newHost();
    Event event = newEvent(5);
    assertThrows(UserNotAuthorisedException.class,
        () -> service.registerForEvent(host.getId(), event.getId()));
  }

  @Test
  void registerForEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    Student student = newStudent();
    assertThrows(EventNotFoundException.class,
        () -> service.registerForEvent(student.getId(), UUID.randomUUID()));
  }

  @Test
  void registerForEvent_whenAlreadyRegistered_throwsIllegalArgumentException() {
    Student student = newStudent();
    Event event = newEvent(5);
    service.registerForEvent(student.getId(), event.getId());

    assertThrows(IllegalArgumentException.class,
        () -> service.registerForEvent(student.getId(), event.getId()));
  }

  @Test
  void registerForEvent_whenEventIsFull_throwsCapacityExceededException() {
    Event event = newEvent(1);
    service.registerForEvent(newStudent().getId(), event.getId());

    Student overflow = newStudent();
    assertThrows(CapacityExceededException.class,
        () -> service.registerForEvent(overflow.getId(), event.getId()));
  }

  @Test
  void registerForEvent_whenReachingWarningThreshold_publishesCapacityWarningOnce() {
    Event event = newEvent(5); // threshold = ceil(5 * 0.8) = 4

    for (int i = 0; i < 4; i++) {
      service.registerForEvent(newStudent().getId(), event.getId());
    }

    assertEquals(List.of(NotificationType.CAPACITY_WARNING), subscriptionService.publishedReasons);
    assertEquals(List.of(event.getId()), subscriptionService.publishedEvents);
  }

  @Test
  void registerForEvent_belowWarningThreshold_doesNotPublish() {
    Event event = newEvent(5); // threshold = 4

    for (int i = 0; i < 3; i++) {
      service.registerForEvent(newStudent().getId(), event.getId());
    }

    assertTrue(subscriptionService.publishedReasons.isEmpty());
  }

  @Test
  void registerForEvent_unlimitedCapacityEvent_neverFullAndNeverWarns() {
    Event event = newUnlimitedEvent();

    for (int i = 0; i < 3; i++) {
      service.registerForEvent(newStudent().getId(), event.getId());
    }

    assertTrue(subscriptionService.publishedReasons.isEmpty());
  }

  // --- deregisterFromEvent ---

  @Test
  void deregisterFromEvent_removesEventAndUnsubscribes() {
    Student student = newStudent();
    Event event = newEvent(5);
    service.registerForEvent(student.getId(), event.getId());

    service.deregisterFromEvent(student.getId(), event.getId());

    assertFalse(service.getEventsForStudent(student.getId()).contains(event));
    assertEquals(1, subscriptionService.unsubscribeCalls.size());
    RecordingSubscriptionService.Call call = subscriptionService.unsubscribeCalls.get(0);
    assertEquals(student.getId(), call.userId());
    assertEquals(event.getId(), call.eventId());
    assertEquals(SubscriptionSource.REGISTRATION, call.source());
  }

  @Test
  void deregisterFromEvent_whenNotRegistered_throwsIllegalArgumentException() {
    Student student = newStudent();
    Event event = newEvent(5);

    assertThrows(IllegalArgumentException.class,
        () -> service.deregisterFromEvent(student.getId(), event.getId()));
  }

  @Test
  void deregisterFromEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    Student student = newStudent();
    assertThrows(EventNotFoundException.class,
        () -> service.deregisterFromEvent(student.getId(), UUID.randomUUID()));
  }

  // --- getEventsForStudent ---

  @Test
  void getEventsForStudent_returnsAllRegisteredEvents() {
    Student student = newStudent();
    Event first = newEvent(5);
    Event second = newEvent(5);
    service.registerForEvent(student.getId(), first.getId());
    service.registerForEvent(student.getId(), second.getId());

    Set<IEvent> events = service.getEventsForStudent(student.getId());

    assertEquals(2, events.size());
    assertTrue(events.contains(first));
    assertTrue(events.contains(second));
  }

  @Test
  void getEventsForStudent_whenUserIsHost_throwsUserNotAuthorisedException() {
    Host host = newHost();
    assertThrows(UserNotAuthorisedException.class,
        () -> service.getEventsForStudent(host.getId()));
  }

  // --- helpers ---

  private Student newStudent() {
    Student student =
        new Student("student", "student@york.ac.uk", UUID.randomUUID(), new HashSet<>());
    userRepository.save(student);
    return student;
  }

  private Host newHost() {
    Host host = new Host("host", "host@york.ac.uk", new HashSet<>());
    userRepository.save(host);
    return host;
  }

  private Event newEvent(int capacity) {
    Event event = new Event("Event", capacity, EventCategory.MUSIC);
    eventRepository.save(event);
    return event;
  }

  private Event newUnlimitedEvent() {
    Event event = new Event("Event", EventCategory.MUSIC);
    eventRepository.save(event);
    return event;
  }

  /** Records the calls {@link StudentEventService} makes, so they can be asserted directly. */
  private static final class RecordingSubscriptionService extends SubscriptionService {

    private record Call(UUID userId, UUID eventId, SubscriptionSource source) {}

    private final List<Call> subscribeCalls = new ArrayList<>();
    private final List<Call> unsubscribeCalls = new ArrayList<>();
    private final List<UUID> publishedEvents = new ArrayList<>();
    private final List<NotificationType> publishedReasons = new ArrayList<>();

    private RecordingSubscriptionService() {
      super(new InMemorySubscriptionRepository());
    }

    @Override
    public void subscribe(UUID userId, UUID eventId, SubscriptionSource source) {
      subscribeCalls.add(new Call(userId, eventId, source));
    }

    @Override
    public void unsubscribe(UUID userId, UUID eventId, SubscriptionSource source) {
      unsubscribeCalls.add(new Call(userId, eventId, source));
    }

    @Override
    public void publish(UUID eventId, NotificationType reason) {
      publishedEvents.add(eventId);
      publishedReasons.add(reason);
    }
  }
}