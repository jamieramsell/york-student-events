package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import york.studentevents.events.Event;
import york.studentevents.events.EventCategory;
import york.studentevents.events.EventService;
import york.studentevents.events.HostEventService;
import york.studentevents.events.IEventRepository;
import york.studentevents.events.StudentEventService;
import york.studentevents.users.Host;
import york.studentevents.users.IUserRepository;
import york.studentevents.users.Student;
import york.studentevents.users.UserService;
import york.studentevents.venues.Venue;

/**
 * In-process tests for {@link SubprocessResponder}'s handlers against a real, seeded persistence
 * layer.
 *
 * <p>Where {@link SubprocessResponderTest} spawns the responder as a fresh process (with an empty
 * in-memory graph) and can therefore only assert envelope <em>shapes</em>, these tests boot the
 * full service graph over an H2 database (the default {@code test} profile, so the SQL-backed
 * repository beans are active), seed real events and users, and drive
 * {@link SubprocessResponder#route} directly — asserting the responder returns the
 * <em>actual</em> host, start and category, not fixture data.
 * A cross-JVM subprocess could not see data seeded in this test's JVM, which is why the real-data
 * assertions live here rather than in the spawned-process test.
 *
 * <p>Each test runs in a rolled-back transaction, so the seeded rows never leak between cases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SubprocessResponderInProcessTest {

  @Autowired private UserService userService;
  @Autowired private EventService eventService;
  @Autowired private StudentEventService studentEventService;
  @Autowired private HostEventService hostEventService;
  @Autowired private IEventRepository eventRepository;
  @Autowired private IUserRepository userRepository;

  @PersistenceContext private EntityManager entityManager;

  private SubprocessResponder responder;

  @BeforeEach
  void setUp() {
    responder = new SubprocessResponder(
        userService, eventService, studentEventService, hostEventService);
  }

  // Seeding helpers //

  /** Persists a venue to H2 (to satisfy the {@code event.venue_id} FK) and returns its ID. */
  private UUID persistVenue() { // TODO: refactor to use persistent venue repo when one exists
    Venue venue = new Venue("Test Venue", "1 Test Street", 100);
    entityManager.persist(venue);
    entityManager.flush();
    return venue.getId();
  }

  /** Persists an event with a venue and a future start/end, and returns it. */
  private Event persistEvent(EventCategory category) {
    Event event = new Event("Test Event", category);
    event.setVenue(persistVenue());
    LocalDateTime start = LocalDateTime.now().plusDays(7);
    event.setDateTime(start, start.plusHours(2));
    eventRepository.save(event);
    return event;
  }

  /** Persists a host that hosts the given events. */
  private Host persistHost(UUID... hostedEventIds) {
    Host host = new Host("hostname", "host@york.ac.uk", "hash", Set.of(hostedEventIds));
    userRepository.save(host);
    return host;
  }

  /** Persists a student registered for the given events. */
  private Student persistStudent(UUID... registeredEventIds) {
    Student student =
        new Student("student", "student@york.ac.uk", "hash", Set.of(registeredEventIds));
    userRepository.save(student);
    return student;
  }

  private static String eventInfoRequest(UUID eventId) {
    return String.format(
        "{\"requestType\":\"GET_EVENT_INFO\",\"payload\":{\"eventId\":\"%s\"}}", eventId);
  }

  private static String batchRequest(UUID... eventIds) {
    StringBuilder ids = new StringBuilder();
    for (int i = 0; i < eventIds.length; i++) {
      if (i > 0) {
        ids.append(",");
      }
      ids.append("\"").append(eventIds[i]).append("\"");
    }
    return String.format(
        "{\"requestType\":\"GET_BATCH_EVENT_INFO\",\"payload\":{\"eventIds\":[%s]}}", ids);
  }

  private static String userEventsRequest(UUID userId) {
    return String.format(
        "{\"requestType\":\"GET_USER_EVENTS\",\"payload\":{\"userId\":\"%s\"}}", userId);
  }

  private static String badgeAwardedRequest(UUID userId, String badgeName) {
    return String.format(
        "{\"requestType\":\"BADGE_AWARDED\",\"payload\":{\"userId\":\"%s\",\"badgeName\":\"%s\"}}",
        userId, badgeName);
  }

  /** Deserialises the request line and routes it, returning the parsed JSON response. */
  private JsonObject handle(String requestLine) {
    String response = responder.route(SubprocessResponder.deserialiseEnvelope(requestLine));
    return JsonParser.parseString(response).getAsJsonObject();
  }

  // GET_EVENT_INFO //

  @Test
  void getEventInfoReturnsRealHostStartAndCategory() {
    Event event = persistEvent(EventCategory.SOCIAL);
    Host host = persistHost(event.getId());

    JsonObject response = handle(eventInfoRequest(event.getId()));

    assertEquals("ok", response.get("status").getAsString());
    JsonObject payload = response.getAsJsonObject("payload");
    assertEquals(host.getId().toString(), payload.get("host").getAsString());
    // Assert against what the service actually returns, so persistence precision can't flake it.
    assertEquals(
        eventService.getEvent(event.getId()).getStartDateTime().toString(),
        payload.get("start").getAsString());
    assertEquals("SOCIAL", payload.get("category").getAsString());
  }

  @Test
  void getEventInfoForUnknownEventThrows() {
    // route() surfaces a not-found as IllegalArgumentException; main() wraps it as an error
    // envelope.
    IllegalArgumentException error = assertThrows(
        IllegalArgumentException.class,
        () -> handle(eventInfoRequest(UUID.randomUUID())));
    assertTrue(error.getMessage().contains("not recognised"));
  }

  // GET_BATCH_EVENT_INFO //

  @Test
  void getBatchEventInfoReturnsPerEventInfoKeyedByIdInRequestOrder() {
    Event social = persistEvent(EventCategory.SOCIAL);
    Event academic = persistEvent(EventCategory.ACADEMIC);
    persistHost(social.getId());
    persistHost(academic.getId());

    JsonObject events = handle(batchRequest(academic.getId(), social.getId()))
        .getAsJsonObject("payload").getAsJsonObject("events");

    assertEquals(2, events.size());
    assertEquals(
        "ACADEMIC",
        events.getAsJsonObject(academic.getId().toString()).get("category").getAsString());
    assertEquals(
        "SOCIAL",
        events.getAsJsonObject(social.getId().toString()).get("category").getAsString());
  }

  // GET_USER_EVENTS //

  @Test
  void getUserEventsForStudentReturnsRegisteredEventIds() {
    Event event = persistEvent(EventCategory.SOCIAL);
    Student student = persistStudent(event.getId());

    JsonObject response = handle(userEventsRequest(student.getId()));

    assertEquals("ok", response.get("status").getAsString());
    var events = response.getAsJsonObject("payload").getAsJsonArray("events");
    assertEquals(1, events.size());
    assertEquals(event.getId().toString(), events.get(0).getAsString());
  }

  @Test
  void getUserEventsForHostReturnsHostedEventIds() {
    Event event = persistEvent(EventCategory.SOCIAL);
    Host host = persistHost(event.getId());

    JsonObject response = handle(userEventsRequest(host.getId()));

    assertEquals("ok", response.get("status").getAsString());
    var events = response.getAsJsonObject("payload").getAsJsonArray("events");
    assertEquals(1, events.size());
    assertEquals(event.getId().toString(), events.get(0).getAsString());
  }

  // BADGE_AWARDED //

  @Test
  void badgeAwardedForKnownUserReturnsEmptyOkPayload() {
    Student student = persistStudent();

    JsonObject response = handle(badgeAwardedRequest(student.getId(), "First Event"));

    assertEquals("ok", response.get("status").getAsString());
    assertTrue(response.getAsJsonObject("payload").isEmpty());
  }
}
