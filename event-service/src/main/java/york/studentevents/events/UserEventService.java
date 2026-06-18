package york.studentevents.events;

import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
     * @param userId the user's ID
     * @param eventId the event's ID
     * <p>
     * @throws IllegalArgumentException if the user or event does not exist, or if the user is already registered for the event.
     * @throws CapacityExceededException if the event is full
     * */
    void registerForEvent(UUID userId, UUID eventId) throws CapacityExceededException, IllegalArgumentException {
        Optional<IEvent> optionalIEvent = eventRepository.findByID(eventId);
        Optional<IUser> optionalIUser = userRepository.findByID(userId);
        if (optionalIEvent.isEmpty()) throw new IllegalArgumentException("Event does not exist");
        if (optionalIUser.isEmpty()) throw new IllegalArgumentException("User does not exist");
        if (optionalIUser.get().getRegisteredEvents().contains(eventId)) throw new IllegalArgumentException("User is already registered for this event");
        if (optionalIEvent.get().getCapacity() <= 0) throw new CapacityExceededException("Event is full");

        IEvent event = optionalIEvent.get();
        IUser user = optionalIUser.get();

        user.getRegisteredEvents().add(eventId);
    }

    /**
     * Deregister a user from an event
     * @param userId the user's ID
     * @param eventId the event's ID
     * <p>
     * @throws IllegalArgumentException if the user or event does not exist, or if the user is not registered for the event.
     * */
    void deregisterFromEvent(UUID userId, UUID eventId) throws IllegalArgumentException {
        Optional<IUser> optionalIUser = userRepository.findByID(userId);
        if (optionalIUser.isEmpty()) throw new IllegalArgumentException("User does not exist");
        if (!optionalIUser.get().getRegisteredEvents().contains(eventId)) throw new IllegalArgumentException("User is not registered for this event");

        IUser user = optionalIUser.get();
        user.getRegisteredEvents().remove(eventId);
    }

    /**
     * Get a list of events a user is registered for
     *
     * @param userId the user's ID
     *
     * @throws IllegalArgumentException if the user, or any of the events, does not exist.
     */
    List<IEvent> getEventsForUser(UUID userId) throws IllegalArgumentException {
        Optional<IUser> optionalIUser = userRepository.findByID(userId);
        if (optionalIUser.isEmpty()) throw new IllegalArgumentException("User does not exist");

        IUser user = optionalIUser.get();
        List<UUID> events = user.getRegisteredEvents();
        return events.stream()
                .map(eventId -> eventRepository.findByID(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("")))
                .toList();
    }

}
