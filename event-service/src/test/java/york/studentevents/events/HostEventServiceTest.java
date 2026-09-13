package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.subscriptions.SubscriptionService;
import york.studentevents.users.Host;
import york.studentevents.users.IHost;
import york.studentevents.users.Student;

/** Tests {@link HostEventService} against real in-memory repositories and a recording
 * subscription service. */
class HostEventServiceTest {

  private InMemoryUserRepository userRepository;
  private IEventRepository eventRepository;
  private EventService eventService;
  private HostEventService hostEventService;

  @BeforeEach
  void setUp() {
    userRepository = new InMemoryUserRepository();
    eventRepository = new InMemoryEventRepository();
    eventService = new EventService(eventRepository);
    hostEventService = new HostEventService(userRepository, eventService);
  }

  // --- registerForEvent ---

  @Test
  void registerForEvent_addsEventToHost() {
    Host host = newHost();
    Event event = newEvent(5);

    hostEventService.registerForEvent(host.getId(), event.getId());
    assertTrue(hostEventService.getEventsForHost(host.getId()).contains(event));
  }

  @Test
  void registerForEvent_whenUserDoesNotExist_throwsUserNotFoundException() {
    Event event = newEvent(5);
    assertThrows(UserNotFoundException.class,
        () -> hostEventService.registerForEvent(UUID.randomUUID(), event.getId()));
  }

  @Test
  void registerForEvent_whenUserIsStudent_throwsUserNotAuthorisedException() {
    Student student = newStudent();
    Event event = newEvent(5);
    assertThrows(UserNotAuthorisedException.class,
        () -> hostEventService.registerForEvent(student.getId(), event.getId()));
  }

  @Test
  void registerForEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    Host host = newHost();
    assertThrows(EventNotFoundException.class,
        () -> hostEventService.registerForEvent(host.getId(), UUID.randomUUID()));
  }

  @Test
  void registerForEvent_whenAlreadyRegistered_throwsIllegalArgumentException() {
    Host host = newHost();
    Event event = newEvent(5);
    hostEventService.registerForEvent(host.getId(), event.getId());

    assertThrows(IllegalArgumentException.class,
        () -> hostEventService.registerForEvent(host.getId(), event.getId()));
  }

  // --- deregisterFromEvent ---

  @Test
  void deregisterFromEvent_removesEventFromHost() {
    Host host = newHost();
    Event event = newEvent(5);

    hostEventService.registerForEvent(host.getId(), event.getId());
    hostEventService.deregisterFromEvent(host.getId(), event.getId());
    assertFalse(hostEventService.getEventsForHost(host.getId()).contains(event));
  }

  @Test
  void deregisterFromEvent_whenNotRegistered_throwsIllegalArgumentException() {
    Host host = newHost();
    Event event = newEvent(5);

    assertThrows(IllegalArgumentException.class,
        () -> hostEventService.deregisterFromEvent(host.getId(), event.getId()));
  }

  @Test
  void deregisterFromEvent_whenUserDoesNotExist_throwsUserNotFoundException() {
    Event event = newEvent(1);
    assertThrows(UserNotFoundException.class,
        () -> hostEventService.deregisterFromEvent(UUID.randomUUID(), event.getId()));
  }

  @Test
  void deregisterFromEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    Host host = newHost();
    assertThrows(EventNotFoundException.class,
        () -> hostEventService.deregisterFromEvent(host.getId(), UUID.randomUUID()));
  }

  // --- getEventsForHost ---

  @Test
  void getEventsForHost_returnsAllRegisteredEvents() {
    Host host = newHost();
    Event first = newEvent(5);
    Event second = newEvent(5);
    hostEventService.registerForEvent(host.getId(), first.getId());
    hostEventService.registerForEvent(host.getId(), second.getId());

    Set<IEvent> events = hostEventService.getEventsForHost(host.getId());

    assertEquals(2, events.size());
    assertTrue(events.contains(first));
    assertTrue(events.contains(second));
  }

  @Test
  void getEventsForHost_whenHostHasNoEvents_ReturnsEmptySet() {
    Host host = newHost();
    Set<IEvent> events = hostEventService.getEventsForHost(host.getId());
    assertEquals(0, events.size());
  }

  @Test
  void getEventsForHost_whenUserIsStudent_throwsUserNotAuthorisedException() {
    Student student = newStudent();
    assertThrows(UserNotAuthorisedException.class,
        () -> hostEventService.getEventsForHost(student.getId()));
  }

  @Test
  void getEventsForHost_whenUserDoesNotExist_throwsUserNotFoundException() {
    assertThrows(UserNotFoundException.class,
        () -> hostEventService.getEventsForHost(UUID.randomUUID()));
  }

  // --- getHostsForEvent ---

  @Test
  void getHostsForEvent_returnsAllRegisteredHosts() {
    Event event = newEvent(5);
    Host first = newHost();
    Host second = newHost();
    hostEventService.registerForEvent(first.getId(), event.getId());
    hostEventService.registerForEvent(second.getId(), event.getId());

    Set<IHost> hosts = hostEventService.getHostsForEvent(event.getId());

    assertEquals(2, hosts.size());
    assertTrue(hosts.contains(first));
    assertTrue(hosts.contains(second));
  }

  @Test
  @SuppressWarnings("unlikely-arg-type")
  void getHostsForEvent_doesNotIncludeStudents() {
    StudentEventService studentEventService = new StudentEventService(
        eventRepository,
        userRepository, 
        new SubscriptionService(new InMemorySubscriptionRepository()), 
        eventService);

    Event event = newEvent(5);
    Host first = newHost();
    Host second = newHost();
    Student student = newStudent();

    hostEventService.registerForEvent(first.getId(), event.getId());
    hostEventService.registerForEvent(second.getId(), event.getId());
    studentEventService.registerForEvent(student.getId(), event.getId());

    Set<IHost> hosts = hostEventService.getHostsForEvent(event.getId());

    assertEquals(2, hosts.size());
    assertTrue(hosts.contains(first));
    assertTrue(hosts.contains(second));
    assertFalse(hosts.contains(student));
  }

  @Test
  void getHostsForEvent_whenEventHasNoHosts_ReturnsEmptySet() {
    Event event = newEvent(1);
    Set<IHost> hosts = hostEventService.getHostsForEvent(event.getId());
    assertEquals(0, hosts.size());
  }

  @Test
  void getHostsForEvent_whenEventDoesNotExist_throwsEventNotFoundException() {
    assertThrows(EventNotFoundException.class,
        () -> hostEventService.getHostsForEvent(UUID.randomUUID()));
  }

  // --- Helpers ---

  private Student newStudent() {
    Student student =
        new Student("student", "student@york.ac.uk", "hash", new HashSet<>());
    userRepository.save(student);
    return student;
  }

  private Host newHost() {
    Host host = new Host("host", "host@york.ac.uk", "hash", new HashSet<>());
    userRepository.save(host);
    return host;
  }

  private Event newEvent(int capacity) {
    Event event = new Event("Event", capacity, EventCategory.MUSIC);
    eventRepository.save(event);
    return event;
  }

}