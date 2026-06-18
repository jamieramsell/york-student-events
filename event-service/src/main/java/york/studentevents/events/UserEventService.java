package york.studentevents.events;

import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service exposing user-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * controllers and repositories. Persistence is delegated to the injected repository;
 * the service holds no state of its own.
 *
 * @see IRepository
 * @see IUserRepository
 * @see IEventRepository
 * @see IUser
 */
public class UserEventService {
    
    private final IEventRepository eventRepository;
    private final IUserRepository userRepository;

    /**
     * Constructor for UserEventService
     * @param eventRepository the event repository which events are registered to
     * @param userRepository the user repository which users are registered to
     */
    public UserEventService(IEventRepository eventRepository, IUserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Register a user for an event
     *
     * @param userId the user's ID
     * @param eventId the event's ID
     * 
     * @throws EventNotFoundException if the event does not exist
     * @throws UserNotFoundException if the user does not exist
     * @throws CapacityExceededException if the event is full
     * @throws IllegalArgumentException if the user is already registered for the event
     * */
    void registerForEvent(UUID userId, UUID eventId) throws CapacityExceededException {
        Optional<IEvent> optionalIEvent = eventRepository.findByID(eventId);
        Optional<IUser> optionalIUser = userRepository.findByID(userId);

        // Validation
        if (optionalIEvent.isEmpty()) {
            throw new EventNotFoundException("Event does not exist");
        } else if (optionalIUser.isEmpty()) {
            throw new UserNotFoundException("User does not exist");
        } else if (optionalIUser.get().getRegisteredEvents().contains(eventId)) {
            throw new IllegalArgumentException("User is already registered for this event");
        } else if (optionalIEvent.get().getCapacity() <= 0) {
            throw new CapacityExceededException("Event is full");
        }

        // Update user and place back in repository
        IUser user = optionalIUser.get();
        user.getRegisteredEvents().add(eventId);
        userRepository.save(user);
    }

    /**
     * Deregister a user from an event
     * 
     * @param userId the user's ID
     * @param eventId the event's ID
     * 
     * @throws IllegalArgumentException if the user is not registered for the event
     * @throws UserNotFoundException if the user does not exist
     */
    void deregisterFromEvent(UUID userId, UUID eventId) throws IllegalArgumentException {
        Optional<IUser> optionalIUser = userRepository.findByID(userId);

        // Validation
        if (optionalIUser.isEmpty()) {
            throw new UserNotFoundException("User does not exist");
        } else if (!optionalIUser.get().getRegisteredEvents().contains(eventId)) {
            throw new IllegalArgumentException("User is not registered for this event");
        }

        // Update user and place back in repository
        IUser user = optionalIUser.get();
        user.getRegisteredEvents().remove(eventId);
        userRepository.save(user);
    }

    /**
     * Get a list of events a user is registered for
     * 
     * @param userId the user's ID
     * 
     * @return a list of events the user is registered for
     * 
     * @throws IllegalArgumentException if the user does not exist
     * @throws UserNotFoundException if the user does not exist
     */
    List<IEvent> getEventsForUser(UUID userId) throws IllegalArgumentException {
        Optional<IUser> optionalIUser = userRepository.findByID(userId);
        if (optionalIUser.isEmpty()) {
            throw new UserNotFoundException("User does not exist");
        }
        IUser user = optionalIUser.get();

        /*
         * Get a stream of event IDs and map them onto their actual entities. If an event ID points
         * to an event which does not exist, throw an error.
         */
        List<UUID> events = user.getRegisteredEvents();
        List<IEvent> userEvents = events.stream()
                .map(eventId -> eventRepository.findByID(eventId)
                        .orElseThrow(() -> new EventNotFoundException("")))
                .toList();
        return userEvents;
    }
}
