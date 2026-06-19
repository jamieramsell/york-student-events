package york.studentevents.cohorts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;

/**
 * Application service exposing cohort-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * {@link CohortController} and the {@code IRepository} later. Persistence is delegated
 * to the injected repositories; the service holds no state of its own.
 *
 * @see IRepository
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
   * @see IRepository
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
   * Assigns a user to a cohort.
   *
   * @param userId the user's ID
   * @param cohortId the cohort's ID
   *
   * @throws UserNotFoundException if the user is not found
   * @throws CohortNotFoundException if the cohort is not found
   * @throws IllegalArgumentException if the user is already in the cohort
   * */
  void assignUserToCohort(UUID userId, UUID cohortId) {
    Optional<IUser> optionalUser = userRepository.findByID(userId);
    Optional<ICohort> optionalCohort = cohortRepository.findByID(cohortId);

    // Validation
    if (optionalUser.isEmpty()) {
      throw new UserNotFoundException("User not found");
    } else if (optionalCohort.isEmpty()) {
      throw new CohortNotFoundException("Cohort not found");
    }
    
    ICohort cohort = optionalCohort.get();

    // Check member is not already in the cohort before assigning
    if (cohort.getMembers().contains(userId)) {
      throw new IllegalArgumentException("User is already in the cohort");
    }
    cohort.getMembers().add(userId);
  }

  /**
   * Gets a list of users in a cohort.
   *
   * @param cohortId the cohort's ID
   *
   * @return a list of users in the cohort
   *
   * @throws CohortNotFoundException if the cohort is not found
   * @throws UserNotFoundException if a user in the cohort is not found
   *
   * @see IUser
   */
  List<IUser> getUsersForCohort(UUID cohortId) {
    Optional<ICohort> optionalCohort = cohortRepository.findByID(cohortId);
    if (optionalCohort.isEmpty()) {
      throw new CohortNotFoundException("Cohort not found");
    }

    /* 
     * Map cohort members to a list and return. If a user exists within the cohort
     * who does not exist, throw an error.
     */
    ICohort cohort = optionalCohort.get();
    List<IUser> cohortMembers = cohort.getMembers().stream()
        .map(userId -> userRepository.findByID(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found")))
        .toList();
    return cohortMembers;
  }

  /**
   * Gets a list of events for a cohort.
   *
   * @param cohortId the cohort's ID
   *
   * @return a list of events for the cohort
   *
   * @throws CohortNotFoundException if the cohort is not found
   * @throws EventNotFoundException if an event in the cohort is not found
   */
  List<IEvent> getEventsForCohort(UUID cohortId) {
    /* 
     * Map cohort events to a list with no repeats and return. If an ID exists within
     * the cohort which does not point to an actual event, throw an error.
     */
    List<IUser> members = getUsersForCohort(cohortId);
    List<IEvent> cohortEvents = members.stream()
        .map(IUser::getRegisteredEvents)
        .distinct()
        .flatMap(List::stream)
        .map(eventId -> eventRepository.findByID(eventId)
            .orElseThrow(() -> new EventNotFoundException("Event not found")))
        .toList();
    return cohortEvents;
  }

}
