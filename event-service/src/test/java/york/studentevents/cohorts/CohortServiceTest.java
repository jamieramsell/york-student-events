package york.studentevents.cohorts;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import york.studentevents.events.EventService;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.pages.IllegalPageNumberException;
import york.studentevents.exceptions.pages.IllegalPageSizeException;
import york.studentevents.repository.inmemory.InMemoryCohortRepository;
import york.studentevents.repository.inmemory.InMemoryEventRepository;
import york.studentevents.repository.inmemory.InMemoryUserRepository;
import york.studentevents.users.UserService;

public class CohortServiceTest {

  private InMemoryCohortRepository cohortRepository;
  private InMemoryEventRepository eventRepository;
  private InMemoryUserRepository userRepository;
  private UserService userService;
  private EventService eventService;
  private CohortService cohortService;

  private UUID cohort;

  @BeforeEach
  void setUp() {
    cohortRepository = new InMemoryCohortRepository();
    eventRepository = new InMemoryEventRepository();
    userRepository = new InMemoryUserRepository();
    userService = new UserService(userRepository);
    eventService = new EventService(eventRepository);
    cohortService = new CohortService(cohortRepository, userRepository, eventRepository);

    cohort = UUID.randomUUID();
  }

  // --- constructor ---

  // --- assignStudentToCohort ---

  // --- removeStudentFromCohort ---

  // --- getAllCohortsForStudent ---
  @Test
  void getAllStudentsOfCohort_returnsEmptyPage_forEmptyCohort() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    cohortService.getAllStudentsOfCohort(cohort, 0, 10);
  }

  @Test
  void getAllStudentsOfCohort_throwsCohortNotFound_forNonExistentCohort() {
    assertThrows(CohortNotFoundException.class, () -> cohortService.getAllStudentsOfCohort(UUID.randomUUID(), 0, 10));
  }

  @Test
  void getAllStudentsOfCohort_throwsIllegalPageSize_forZeroPageSize() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    assertThrows(IllegalPageSizeException.class, () -> cohortService.getAllStudentsOfCohort(cohort, 0, 0));
  }

  @Test
  void getAllStudentsOfCohort_throwsIllegalPageSize_forPageSizeToLarge() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    assertThrows(IllegalPageSizeException.class, () -> cohortService.getAllStudentsOfCohort(cohort, 0, 1000));
  }

  @Test
  void getAllStudentsOfCohort_throwsIllegalPageNumber_forNegativePageNumber() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    assertThrows(IllegalPageNumberException.class, () -> cohortService.getAllStudentsOfCohort(cohort, -1, 10));
  }

  // --- getAllEventsForCohort ---
  @Test
  void getAllEventsForCohort_returnsEmptyPage_forEmptyCohort() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    cohortService.getAllEventsForCohort(cohort, 0, 10);
  }

  @Test
  void getAllEventsForCohort_throwsCohortNotFound_forNonExistentCohort() {
    assertThrows(CohortNotFoundException.class, () -> cohortService.getAllEventsForCohort(UUID.randomUUID(), 0, 10));
  }

  @Test
  void getAllEventsForCohort_throwsIllegalPageSize_forZeroPageSize() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    assertThrows(IllegalPageSizeException.class, () -> cohortService.getAllEventsForCohort(cohort, 0, 0));
  }

  @Test
  void getAllEventsForCohort_throwsIllegalPageSize_forPageSizeToLarge() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    assertThrows(IllegalPageSizeException.class, () -> cohortService.getAllEventsForCohort(cohort, 0, 1000));
  }

  @Test
  void getAllEventsForCohort_throwsIllegalPageNumber_forNegativePageNumber() {
    cohortRepository.save(new Cohort(cohort, "Test Cohort", "Test Department", 1, 1));
    assertThrows(IllegalPageNumberException.class, () -> cohortService.getAllEventsForCohort(cohort, -1, 10));
  }

}
