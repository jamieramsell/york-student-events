package york.studentevents.cohorts;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.UnauthorizedOperationException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.users.IStudent;
import york.studentevents.users.IUser;
import york.studentevents.users.IUser.UserType;
import york.studentevents.users.IUserRepository;

/**
 * Application service exposing cohort-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * {@link CohortController} and the {@code IRepository} later. Persistence is delegated
 * to the injected repositories; the service holds no state of its own.
 *
 * @see york.studentevents.repository.IRepository
 * @see ICohortRepository
 * @see ICohort
 */
public class CohortService {
  
  private final ICohortRepository cohortRepository;
  private final IUserRepository userRepository;
  private final IEventRepository eventRepository;

  /**
   * Constructor for the CohortService.
   *
   * @param cohortRepository the cohort repository which cohorts are registered to
   * @param userRepository the user repository which users are registered to
   * @param eventRepository the event repository which events are registered to
   *
   * @see york.studentevents.repository.IRepository
   * @see ICohortRepository
   * @see IUserRepository
   * @see IEventRepository
   */
  public CohortService(ICohortRepository cohortRepository, IUserRepository userRepository,
      IEventRepository eventRepository) {
    this.cohortRepository = cohortRepository;
    this.userRepository = userRepository;
    this.eventRepository = eventRepository;
  }

  /**
   * Assigns a Student to a Cohort.
   *
   * @param userId the students's user ID
   * @param cohortId the cohort's ID
   * @throws UserNotFoundException if the user is not found
   * @throws UnauthorizedOperationException if the given user is not a Student
   * @throws CohortNotFoundException if the cohort is not found
   * @throws IllegalArgumentException if the user is already in the cohort
   *
   * @see IStudent
   */
  void assignStudentToCohort(UUID userId, UUID cohortId) {
    Optional<IUser> optionalUser = userRepository.findByID(userId);
    Optional<ICohort> optionalCohort = cohortRepository.findByID(cohortId);

    // Validation
    if (optionalUser.isEmpty()) {
      throw new UserNotFoundException("User not found");
    } else if (optionalUser.get().getType() != UserType.STUDENT) {
      throw new UnauthorizedOperationException("The given user is not a student");
    } else if (optionalCohort.isEmpty()) {
      throw new CohortNotFoundException("Cohort not found");
    }
    
    ICohort cohort = optionalCohort.get();

    boolean successfullyAdded = cohort.getMembers().add(userId);
    if (!successfullyAdded) {
      throw new IllegalArgumentException("Student is already in the cohort");
    }
  }

  /**
   * Gets the set of Students within a Cohort.
   *
   * @param cohortId the cohort's ID
   * @return a set of Students in the Cohort
   * @throws CohortNotFoundException if the cohort is not found
   *
   * @see IStudent
   */
  Set<IStudent> getStudentsOfCohort(UUID cohortId) {
    Optional<ICohort> optionalCohort = cohortRepository.findByID(cohortId);
    if (optionalCohort.isEmpty()) {
      throw new CohortNotFoundException("Cohort not found");
    }

    /* 
     * Map cohort members to a set and return. If a user exists within the cohort who does not
     * exist, throw an error.
     */
    ICohort cohort = optionalCohort.get();
    Set<IStudent> cohortMembers = new HashSet<>(
        cohort.getMembers().stream()
        .map(userId -> userRepository.findByID(userId)
            .orElseThrow(() -> new IllegalStateException("User " + userId + " exists within the"
                + " cohort, but was not found within the user repository")))
        .map(user -> (IStudent) user)
        .toList()
    );
    return cohortMembers;
  }

  /**
   * Retrieves the set of Events for a given Cohort.
   *
   * @param cohortId the cohort's ID
   * @return a set of events for the cohort
   * @throws CohortNotFoundException if the cohort is not found
   */
  Set<IEvent> getEventsForCohort(UUID cohortId) {
    /* 
     * Map cohort events to a set and return. If an ID exists within the cohort which does not point
     * to an actual event, throw an error.
     */
    Set<IStudent> members = getStudentsOfCohort(cohortId);
    Set<IEvent> events = new HashSet<>(
        members.stream()
        .map(student -> student.getRegisteredEvents())
        .flatMap(set -> set.stream())
        .map(eventId -> eventRepository.findByID(eventId)
            .orElseThrow(() -> new IllegalStateException("A student within the cohort is registered"
                + " to event " + eventId + ", which could not be found within the event"
                + " repository.")))
        .toList()
    );
    
    return events;
  }

}
