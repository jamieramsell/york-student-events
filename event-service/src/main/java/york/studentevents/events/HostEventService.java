package york.studentevents.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.users.IHost;
import york.studentevents.users.IUser.UserType;
import york.studentevents.users.IUser;
import york.studentevents.users.IUserRepository;

/**
 * Application service exposing Host-related event operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the controllers and
 * repositories. Persistence is delegated to the injected repository; the service holds no state of
 * its own.
 *
 * @see york.studentevents.repository.IRepository
 * @see IUserRepository
 * @see EventService
 * @see IHost
 */
public class HostEventService {

  private final IUserRepository userRepository;
  private final EventService eventService;

  /**
   * Constructor for HostEventService.
   *
   * @param userRepository the user repository which users are registered to
   * @param eventService the current event service instance
   */
  public HostEventService(
      IUserRepository userRepository,
      EventService eventService
  ) {
    if (
        userRepository == null
        || eventService == null
    ) {
      throw new IllegalArgumentException("Injected repositories and services cannot be null");
    }
    this.userRepository = userRepository;
    this.eventService = eventService;
  }

  /**
   * Register a Host for an event.
   *
   * @param userId the Host's user ID
   * @param eventId the event's ID
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Host
   * @throws EventNotFoundException if the event does not exist
   * @throws IllegalArgumentException if the Host is already registered for the event
   */
  public void registerForEvent(UUID userId, UUID eventId) {
    IHost host = getHost(userId);
    eventService.getEvent(eventId); // Validate that the event actually exists
    Set<UUID> hostEvents = host.getHostedEvents();

    // Ensure that the Host was not already registered for the event
    boolean successfullyAdded = hostEvents.add(eventId);
    if (!successfullyAdded) {
      throw new IllegalArgumentException("The Host is already registered for this event");
    }

    // Update the Hosts's record
    host.setHostedEvents(hostEvents);
    userRepository.save(host);
  }

  /**
   * Deregister a host from an event.
   *
   * @param userId the host's user ID
   * @param eventId the event's ID
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Host
   * @throws EventNotFoundException if the event does not exist.
   * @throws IllegalArgumentException if the host is not registered for the event
   */
  public void deregisterFromEvent(UUID userId, UUID eventId) {
    IHost host = getHost(userId);
    eventService.getEvent(eventId); // Validate that the event actually exists
    Set<UUID> hostEvents = host.getHostedEvents();

    // Ensure that the host was already signed up for the event
    boolean successfullyRemoved = hostEvents.remove(eventId);
    if (!successfullyRemoved) {
      throw new IllegalArgumentException("The host is not registered for the given event");
    }

    // Update the host's record
    host.setHostedEvents(hostEvents);
    userRepository.save(host);
  }

  /**
   * Get the set of events that a Host is registered for.
   *
   * @param userId the Host's user ID
   * @return the set of events that the user is registered for; may be empty, never null
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Host
   */
  public Set<IEvent> getEventsForHost(UUID userId) {
    IHost host = getHost(userId);

    Function<UUID, IEvent> mapEventIdToEvent = eventId -> {
      try {
        return eventService.getEvent(eventId);
      } catch (EventNotFoundException e) {
        throw new IllegalStateException("The given Host is registered for an event with ID" 
            + " " + eventId + ", which could not be found within the event repository.");
      }
    };

    /*
     * Get a stream of event IDs and map them onto their actual entities. If an event ID points
     * to an event which does not exist, throw an error.
     */
    Set<UUID> events = host.getHostedEvents();
    Set<IEvent> hostEvents = new HashSet<>(
        events.stream()
            .map(mapEventIdToEvent)
            .toList()
    );
    return hostEvents;
  }

  /**
   * Returns the hosts for a given event in pages.
   *
   * @param eventId the ID of the target event.
   * @param pageNumber the page number to retrieve.
   * @param pageSize the number of hosts to retrieve per page.
   * @return the set of hosts for a given event; may be empty, never null
   * @throws EventNotFoundException if the event does not exist.
   * @throws IllegalArgumentException if {@code pageNumber} is negative, or if
   *        {@code pageSize} is less than 1.
   */
  public Page<IHost> getAllHostsForEvent(UUID eventId, int pageNumber, int pageSize) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("pageNumber must not be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be greater than zero");
    }
    eventService.getEvent(eventId); // Verify that the event does exist

    Predicate<IUser> isHost = user -> user.getType() == UserType.HOST;
    Predicate<IHost> isHostingSpecifiedEvent = host -> host.getHostedEvents().contains(eventId);
    int currentPage = 0;
    Page<IUser> page;
    List<IUser> users = new ArrayList<>(List.of());

    do {
      page = userRepository.findAll(currentPage, 100);
      users.addAll(page.getContent());
      currentPage++;
    } while (page.hasNext());
    List<IHost> hosts = users.stream()
        .filter(isHost)
        .map(u -> (IHost) u)
        .filter(isHostingSpecifiedEvent)
        .toList();

    long startIndexLong = (long) pageNumber * pageSize;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    if (hosts.isEmpty()) {
      return new PageImpl<>(List.of(), PageRequest.of(pageNumber, pageSize), 0);
    }

    int startIndex = Math.toIntExact(startIndexLong);
    int endIndex = Math.min(startIndex + pageSize, hosts.size());

    List<IHost> pageContent = new ArrayList<>(hosts.subList(startIndex, endIndex)) {};
    return new PageImpl<>(pageContent, pageable, hosts.size());
  }

  /**
   * Retrieves a Host from the injected user repository.
   *
   * @param userId the host's user ID
   * @return the {@code IHost} entity; never null
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Host
   */
  private IHost getHost(UUID userId) {
    // Verify that the user exists within the repository
    Optional<IUser> optionalUser = userRepository.findByID(userId);
    
    if (optionalUser.isEmpty()) {
      throw new UserNotFoundException("User does not exist");
    }

    IUser user = optionalUser.get();

    // Verify that the user is a Host
    if (!(user.getType() == UserType.HOST)) {
      throw new UserNotAuthorisedException("User is not a Host");
    }

    // Cast to a Host entity & return
    return (IHost) user;
  }
  
}

