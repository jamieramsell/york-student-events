package york.studentevents.cohorts;

import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;
import york.studentevents.exceptions.CohortNotFoundException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CohortService {
    private final ICohortRepository cohortRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;

    /**
     * Constructor for the CohortService
     * @param cohortRepository the cohort repository which cohorts are registered to
     * @param userRepository the user repository which users are registered to
     * @param eventRepository the event repository which events are registered to
     */
    public CohortService(ICohortRepository cohortRepository, IUserRepository userRepository, IEventRepository eventRepository) {
        this.cohortRepository = cohortRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Assigns a user to a cohort
     * @param userId the user's ID
     * @param cohortId the cohort's ID
     * @throws UserNotFoundException if the user is not found
     * @throws CohortNotFoundException if the cohort is not found
     * @throws IllegalArgumentException if the user is already in the cohort
     * */
    void assignUserToCohort(UUID userId, UUID cohortId) {
        Optional<IUser> optionalIUser = userRepository.findByID(userId);
        Optional<ICohort> optionalICohort = cohortRepository.findByID(cohortId);

        if (optionalIUser.isEmpty()) throw new UserNotFoundException("User not found");
        if (optionalICohort.isEmpty()) throw new CohortNotFoundException("Cohort not found");

        ICohort cohort = optionalICohort.get();

        if (cohort.getMembers().contains(userId)) throw new IllegalArgumentException("User is already in the cohort");
        cohort.getMembers().add(userId);

    }

    /**
     * Gets a list of users in a cohort
     * @param cohortId the cohort's ID
     * @return a list of users in the cohort
     * @throws CohortNotFoundException if the cohort is not found
     * @throws UserNotFoundException if a user in the cohort is not found
     */
    List<IUser> getUsersForCohort(UUID cohortId) {
        Optional<ICohort> optionalICohort = cohortRepository.findByID(cohortId);
        if (optionalICohort.isEmpty()) throw new CohortNotFoundException("Cohort not found");

        ICohort cohort = optionalICohort.get();
        return cohort.getMembers().stream()
                .map(userId -> userRepository.findByID(userId)
                        .orElseThrow(() -> new UserNotFoundException("User not found")))
                .toList();
    }

    /**
     * Gets a list of events for a cohort
     * @param cohortId the cohort's ID
     * @return a list of events for the cohort
     * @throws CohortNotFoundException if the cohort is not found
     * @throws EventNotFoundException if an event in the cohort is not found
     */
    List<IEvent> getEventsForCohort(UUID cohortId) {
        List<IUser> members = getUsersForCohort(cohortId);
        return members.stream()
                .map(IUser::getRegisteredEvents)
                .distinct()
                .flatMap(List::stream)
                .map(eventId -> eventRepository.findByID(eventId)
                        .orElseThrow(() -> new EventNotFoundException("Event not found")))
                .toList();
    }

}
