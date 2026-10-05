package york.studentevents.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import york.studentevents.cohorts.ICohort;
import york.studentevents.cohorts.ICohortRepository;
import york.studentevents.events.EventCategory;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;
import york.studentevents.repository.inmemory.InMemoryCohortRepository;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemorySubscriptionRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.repository.inmemory.InMemoryVenueRepository;
import york.studentevents.subscriptions.ISubscription;
import york.studentevents.subscriptions.ISubscriptionRepository;
import york.studentevents.users.Host;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;
import york.studentevents.users.Student;
import york.studentevents.venues.IVenue;
import york.studentevents.venues.IVenueRepository;

/**
 * Integration tests for {@link InMemorySeededData}.
 *
 * <p>Tests are grouped into eight sections (A–H) matching the plan agreed in the seed-script
 * feature. The happy-path sections (A–F, H) share a single seed load in {@link #loadSeedData()}.
 * Error-handling tests (G) live in a {@link ErrorHandling} nested class and drive the loader
 * directly against temporary files, isolated from the shared state.
 *
 * <p><strong>Note:</strong> tests in sections F (event extended fields) depend on both the
 * single-call {@code setDateTime} fix and the {@code setVenue} call being present in
 * {@code eventLoader}. Until those changes land, the {@link #loadSeedData()} setup step will throw
 * and the entire class will report as failed — which is the intended signal.
 */
class InMemorySeededDataTest {

  // Venue IDs
  private static final UUID CENTRAL_HALL =
      UUID.fromString("10000000-0000-4000-a000-000000000001");
  private static final UUID CAMPUS_LAKESIDE =
      UUID.fromString("10000000-0000-4000-a000-000000000006");

  // Student IDs
  private static final UUID ALICE =
      UUID.fromString("20000000-0000-4000-a000-000000000001");
  private static final UUID BEN =
      UUID.fromString("20000000-0000-4000-a000-000000000002");
  private static final UUID DEV =
      UUID.fromString("20000000-0000-4000-a000-000000000004");
  private static final UUID FIONA =
      UUID.fromString("20000000-0000-4000-a000-000000000006");

  // Host IDs
  private static final UUID YUSU =
      UUID.fromString("30000000-0000-4000-a000-000000000001");
  private static final UUID DRAMASOC =
      UUID.fromString("30000000-0000-4000-a000-000000000004");

  // Event IDs
  private static final UUID FRESHERS_FAIR =
      UUID.fromString("40000000-0000-4000-a000-000000000001");
  private static final UUID OPEN_MIC_NIGHT =
      UUID.fromString("40000000-0000-4000-a000-000000000004");
  private static final UUID BAKE_SALE =
      UUID.fromString("40000000-0000-4000-a000-000000000008");
  private static final UUID LAKESIDE_FILM =
      UUID.fromString("40000000-0000-4000-a000-000000000009");

  // Cohort IDs
  private static final UUID CS_COHORT =
      UUID.fromString("50000000-0000-4000-a000-000000000001");
  private static final UUID HISTORY_COHORT =
      UUID.fromString("50000000-0000-4000-a000-000000000002");
  private static final UUID MATHS_COHORT =
      UUID.fromString("50000000-0000-4000-a000-000000000003");

  // Subscription IDs
  private static final UUID ALICE_OPEN_MIC_SUB =
      UUID.fromString("70000000-0000-4000-a000-000000000001");

  // Expected counts — derived from seed.json at setup so that adding or removing entities
  // does not require updating these tests.
  private static int expectedVenueCount;
  private static int expectedUserCount;
  private static int expectedStudentCount;
  private static int expectedHostCount;
  private static int expectedEventCount;
  private static int expectedCohortCount;
  private static int expectedSubscriptionCount;

  private static ICohortRepository cohortRepository;
  private static IEventRepository eventRepository;
  private static ISubscriptionRepository subscriptionRepository;
  private static IUserRepository userRepository;
  private static IVenueRepository venueRepository;

  @BeforeAll
  static void loadSeedData() throws FileNotFoundException {
    cohortRepository = new InMemoryCohortRepository();
    eventRepository = new InMemoryEventRepository();
    subscriptionRepository = new InMemorySubscriptionRepository();
    userRepository = new InMemoryUserRepository();
    venueRepository = new InMemoryVenueRepository();

    InMemorySeededData seededData = new InMemorySeededData();
    seededData.cohortRepository = cohortRepository;
    seededData.eventRepository = eventRepository;
    seededData.subscriptionRepository = subscriptionRepository;
    seededData.userRepository = userRepository;
    seededData.venueRepository = venueRepository;

    seededData.run();

    // Parse seed.json once to derive expected counts, keeping count assertions resilient to
    // future seed additions without requiring test changes.
    String json;
    try (Scanner scanner = new Scanner(new File("../data/seed.json"))) {
      json = scanner.useDelimiter("\\A").next();
    }
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    expectedVenueCount = root.getAsJsonArray("venues").size();
    expectedEventCount = root.getAsJsonArray("events").size();
    expectedCohortCount = root.getAsJsonArray("cohorts").size();
    expectedSubscriptionCount = root.getAsJsonArray("subscriptions").size();
    expectedUserCount = root.getAsJsonArray("users").size();
    expectedStudentCount = (int) root.getAsJsonArray("users").asList().stream()
        .filter(u -> "STUDENT".equals(u.getAsJsonObject().get("type").getAsString()))
        .count();
    expectedHostCount = (int) root.getAsJsonArray("users").asList().stream()
        .filter(u -> "HOST".equals(u.getAsJsonObject().get("type").getAsString()))
        .count();
  }

  // A. Entity count verification ///////////////////////////////////////////////////////////////

  @Test
  void loadsAllVenues() {
    assertEquals(expectedVenueCount, venueRepository.findAll().size());
  }

  @Test
  void loadsAllUsers() {
    assertEquals(expectedUserCount, userRepository.findAll().size());
  }

  @Test
  void loadsAllEvents() {
    assertEquals(expectedEventCount, eventRepository.findAll().size());
  }

  @Test
  void loadsAllCohorts() {
    assertEquals(expectedCohortCount, cohortRepository.findAll().size());
  }

  @Test
  void loadsAllSubscriptions() {
    assertEquals(expectedSubscriptionCount, subscriptionRepository.findAll().size());
  }

  // B. Field mapping – spot-check one entity per type /////////////////////////////////////////

  @Test
  void centralHallFieldsAreCorrect() {
    IVenue venue = venueRepository.findByID(CENTRAL_HALL).orElseThrow();
    assertEquals("Central Hall", venue.getName());
    assertEquals("University of York, Heslington, York YO10 5DD", venue.getAddress());
    assertEquals(500, venue.getCapacity());
  }

  @Test
  void aliceStudentFieldsAreCorrect() {
    IUser user = userRepository.findByID(ALICE).orElseThrow();
    assertInstanceOf(Student.class, user);
    assertEquals("alice.chen", user.getUsername());
    assertEquals("ac1234@york.ac.uk", user.getEmail());
    assertEquals(IUser.UserType.STUDENT, user.getType());
    assertEquals(5, ((Student) user).getRegisteredEvents().size());
  }

  @Test
  void yusuHostFieldsAreCorrect() {
    IUser user = userRepository.findByID(YUSU).orElseThrow();
    assertInstanceOf(Host.class, user);
    assertEquals("yusu", user.getUsername());
    assertEquals(IUser.UserType.HOST, user.getType());
    assertEquals(6, ((Host) user).getHostedEvents().size());
  }

  @Test
  void freshersFairFieldsAreCorrect() {
    IEvent event = eventRepository.findByID(FRESHERS_FAIR).orElseThrow();
    assertEquals("Freshers Fair", event.getTitle());
    assertEquals(EventCategory.FRESHERS, event.getCategory());
    assertEquals(500, event.getCapacity());
  }

  @Test
  void csCohortFieldsAreCorrect() {
    ICohort cohort = cohortRepository.findByID(CS_COHORT).orElseThrow();
    assertEquals("2nd Year Computer Science 2025/26", cohort.getName());
    assertEquals("Computer Science", cohort.getDepartment());
    assertEquals(2025, cohort.getAcademicYear());
    assertEquals(2, cohort.getYearGroup());
  }

  @Test
  void aliceOpenMicSubFieldsAreCorrect() {
    ISubscription sub = subscriptionRepository.findByID(ALICE_OPEN_MIC_SUB).orElseThrow();
    assertEquals(ALICE, sub.getUserId());
    assertEquals(OPEN_MIC_NIGHT, sub.getEventId());
    assertEquals(ISubscription.SubscriptionSource.REGISTRATION, sub.getSource());
  }

  // C. Nullable / optional field handling //////////////////////////////////////////////////////

  @Test
  void campusLakesideHasNullCapacity() {
    IVenue venue = venueRepository.findByID(CAMPUS_LAKESIDE).orElseThrow();
    assertNull(venue.getCapacity());
  }

  @Test
  void centralHallHasCapacity() {
    IVenue venue = venueRepository.findByID(CENTRAL_HALL).orElseThrow();
    assertEquals(500, venue.getCapacity());
  }

  @Test
  void bakeSaleHasNullCapacity() {
    IEvent event = eventRepository.findByID(BAKE_SALE).orElseThrow();
    assertNull(event.getCapacity());
  }

  @Test
  void freshersFairHasCapacity() {
    IEvent event = eventRepository.findByID(FRESHERS_FAIR).orElseThrow();
    assertEquals(500, event.getCapacity());
  }

  // D. User type discrimination ////////////////////////////////////////////////////////////////

  @Test
  void allStudentsAreStudentInstances() {
    long count = userRepository.findAll().stream()
        .filter(u -> u.getType() == IUser.UserType.STUDENT)
        .peek(u -> assertInstanceOf(Student.class, u))
        .count();
    assertEquals(expectedStudentCount, count);
  }

  @Test
  void allHostsAreHostInstances() {
    long count = userRepository.findAll().stream()
        .filter(u -> u.getType() == IUser.UserType.HOST)
        .peek(u -> assertInstanceOf(Host.class, u))
        .count();
    assertEquals(expectedHostCount, count);
  }

  @Test
  void eventsSetCorrectlyPerUserType() {
    Student alice = (Student) userRepository.findByID(ALICE).orElseThrow();
    assertTrue(alice.getRegisteredEvents().contains(FRESHERS_FAIR));
    Host yusu = (Host) userRepository.findByID(YUSU).orElseThrow();
    assertTrue(yusu.getHostedEvents().contains(FRESHERS_FAIR));
  }

  // E. Cohort member loading ///////////////////////////////////////////////////////////////////

  @Test
  void csCohortHasCorrectMembers() {
    ICohort cohort = cohortRepository.findByID(CS_COHORT).orElseThrow();
    Set<UUID> members = cohort.getMembers();
    assertEquals(3, members.size());
    assertTrue(members.contains(ALICE));
    assertTrue(members.contains(BEN));
    assertTrue(members.contains(DEV));
  }

  @Test
  void mathsCohortHasNoMembers() {
    ICohort cohort = cohortRepository.findByID(MATHS_COHORT).orElseThrow();
    assertTrue(cohort.getMembers().isEmpty());
  }

  // F. Event extended fields ///////////////////////////////////////////////////////////////////
  //
  // These tests require the eventLoader to call setVenue() and use a single combined
  // setDateTime(start, end) call. Until that fix lands, @BeforeAll will throw and the
  // whole class reports as failed — that failure is the signal to fix the loader.

  @Test
  void eventDescriptionIsLoaded() {
    IEvent event = eventRepository.findByID(FRESHERS_FAIR).orElseThrow();
    assertEquals(
        "Browse over 200 student societies, sports clubs, and campus services all under one roof.",
        event.getDescription());
  }

  @Test
  void eventVenueIdIsLoaded() {
    IEvent event = eventRepository.findByID(FRESHERS_FAIR).orElseThrow();
    assertEquals(CENTRAL_HALL, event.getVenue());
  }

  @Test
  void eventStartAndEndDateTimeAreLoaded() {
    IEvent event = eventRepository.findByID(FRESHERS_FAIR).orElseThrow();
    assertEquals(LocalDateTime.of(2026, 9, 21, 10, 0), event.getStartDateTime());
    assertEquals(LocalDateTime.of(2026, 9, 21, 16, 0), event.getEndDateTime());
  }

  @Test
  void eventWithoutCapacityHasOtherFieldsPopulated() {
    IEvent event = eventRepository.findByID(BAKE_SALE).orElseThrow();
    assertNull(event.getCapacity());
    assertNotNull(event.getDescription());
    assertNotNull(event.getVenue());
    assertNotNull(event.getStartDateTime());
    assertNotNull(event.getEndDateTime());
  }

  // G. Error handling //////////////////////////////////////////////////////////////////////////

  /**
   * Drives the loader against temporary files to test the error paths without touching the real
   * seed file.
   *
   * <p>These tests require {@link InMemorySeededData} to expose a package-private
   * {@code seedFilePath} field (defaulting to {@code "../data/seed.json"}) so that each test can
   * inject an absolute path to a temp file. Until that field exists, all tests here are
   * {@link Disabled}. Once added, remove the {@code @Disabled} annotation from the class.
   *
   * <p>The production change needed in {@code InMemorySeededData}:
   * <pre>
   *   // package-private — overridable in tests
   *   String seedFilePath = "../data/seed.json";
   * </pre>
   * And in {@code run()}: {@code File myObj = new File(seedFilePath);}
   */
  @Nested
  class ErrorHandling {

    @TempDir
    Path tempDir;

    private InMemorySeededData freshSeededData() {
      InMemorySeededData sd = new InMemorySeededData();
      sd.cohortRepository = new InMemoryCohortRepository();
      sd.eventRepository = new InMemoryEventRepository();
      sd.subscriptionRepository = new InMemorySubscriptionRepository();
      sd.userRepository = new InMemoryUserRepository();
      sd.venueRepository = new InMemoryVenueRepository();
      return sd;
    }

    @Test
    void missingFileThrowsFileNotFoundException() {
      InMemorySeededData sd = freshSeededData();
      sd.seedFilePath = tempDir.resolve("data/seed.json").toString();
      assertThrows(FileNotFoundException.class, sd::run);
    }

    @Test
    void invalidJsonThrowsIllegalArgumentException() throws IOException {
      Files.createDirectories(tempDir.resolve("data"));
      Files.writeString(tempDir.resolve("data/seed.json"), "not valid json {{{");
      InMemorySeededData sd = freshSeededData();
      sd.seedFilePath = tempDir.resolve("data/seed.json").toString();
      assertThrows(IllegalArgumentException.class, sd::run);
    }

    @Test
    void jsonArrayInsteadOfObjectThrowsIllegalArgumentException() throws IOException {
      Files.createDirectories(tempDir.resolve("data"));
      Files.writeString(tempDir.resolve("data/seed.json"), "[1, 2, 3]");
      InMemorySeededData sd = freshSeededData();
      sd.seedFilePath = tempDir.resolve("data/seed.json").toString();
      assertThrows(IllegalArgumentException.class, sd::run);
    }

    @Test
    void jsonPrimitiveInsteadOfObjectThrowsIllegalArgumentException() throws IOException {
      Files.createDirectories(tempDir.resolve("data"));
      Files.writeString(tempDir.resolve("data/seed.json"), "\"just a string\"");
      InMemorySeededData sd = freshSeededData();
      sd.seedFilePath = tempDir.resolve("data/seed.json").toString();
      assertThrows(IllegalArgumentException.class, sd::run);
    }
  }

  // H. Idempotency and ordering ////////////////////////////////////////////////////////////////

  @Test
  void callingRunAgainDoesNotDuplicateEntities() throws FileNotFoundException {
    ICohortRepository cr = new InMemoryCohortRepository();
    IEventRepository er = new InMemoryEventRepository();
    ISubscriptionRepository sr = new InMemorySubscriptionRepository();

    InMemorySeededData sd = new InMemorySeededData();
    sd.cohortRepository = cr;
    sd.eventRepository = er;
    sd.subscriptionRepository = sr;

    IUserRepository ur = new InMemoryUserRepository();
    IVenueRepository vr = new InMemoryVenueRepository();
    sd.userRepository = ur;
    sd.venueRepository = vr;

    sd.run();
    sd.run();

    assertEquals(expectedVenueCount, vr.findAll().size());
    assertEquals(expectedUserCount, ur.findAll().size());
    assertEquals(expectedEventCount, er.findAll().size());
    assertEquals(expectedCohortCount, cr.findAll().size());
    assertEquals(expectedSubscriptionCount, sr.findAll().size());
  }

  @Test
  void eventVenueIdsReferenceExistingVenues() {
    eventRepository.findAll().forEach(event -> {
      if (event.getVenue() != null) {
        assertTrue(
            venueRepository.findByID(event.getVenue()).isPresent(),
            "Venue " + event.getVenue() + " referenced by event " + event.getId() + " not found");
      }
    });
  }
}
