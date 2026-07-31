package york.studentevents.events;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import york.studentevents.exceptions.CapacityExceededException;
import york.studentevents.exceptions.EventNotFoundException;
import york.studentevents.exceptions.UserNotAuthorisedException;
import york.studentevents.exceptions.UserNotFoundException;
import york.studentevents.subscriptions.ISubscription.SubscriptionSource;
import york.studentevents.subscriptions.NotificationType;
import york.studentevents.subscriptions.SubscriptionService;
import york.studentevents.users.IStudent;
import york.studentevents.users.IUser;
import york.studentevents.users.IUser.UserType;
import york.studentevents.users.IUserRepository;

/**
 * Application service exposing user-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the controllers and
 * repositories. Persistence is delegated to the injected repository; the service holds no state of
 * its own.
 *
 * @see york.studentevents.repository.IRepository
 * @see IUserRepository
 * @see IEventRepository
 * @see IStudent
 */
public class StudentEventService {
  
  private static final double CAPACITY_WARNING_THRESHOLD = 0.8;

  private final IEventRepository eventRepository;
  private final IUserRepository userRepository;
  private final SubscriptionService subscriptionService;
  private final EventService eventService;

  /**
   * Constructor for StudentEventService.
   *
   * @param eventRepository the event repository which events are registered to
   * @param userRepository the user repository which users are registered to
   * @param subscriptionService the current subscription service instance
   * @param eventService the current event service instance
   */
  public StudentEventService(
      IEventRepository eventRepository,
      IUserRepository userRepository,
      SubscriptionService subscriptionService,
      EventService eventService
  ) {
    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
    this.subscriptionService = subscriptionService;
    this.eventService = eventService;
  }

  /**
   * Register a student for an event.
   *
   * @param userId the student's user ID
   * @param eventId the event's ID
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   * @throws EventNotFoundException if the event does not exist
   * @throws IllegalArgumentException if the student is already registered for the event
   * @throws CapacityExceededException if the event is full
   */
  public void registerForEvent(UUID userId, UUID eventId) {
    IStudent student = getStudent(userId);

    if (isEventFull(eventId)) {
      throw new CapacityExceededException("The given event is full to capacity.");
    }

    Set<UUID> studentEvents = student.getRegisteredEvents();

    // Ensure that the student was not already registered for the event
    boolean successfullyAdded = studentEvents.add(eventId);
    if (!successfullyAdded) {
      throw new IllegalArgumentException("The student is already registered for this event");
    }

    // Update the student's record
    student.setRegisteredEvents(studentEvents);
    userRepository.save(student);
    subscriptionService.subscribe(userId, eventId, SubscriptionSource.REGISTRATION);
    publishCapacityWarningIfReached(eventId);
  }

  /**
   * Deregister a student from an event.
   *
   * @param userId the student's user ID
   * @param eventId the event's ID
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   * @throws EventNotFoundException if the event does not exist.
   * @throws IllegalArgumentException if the student is not registered for the event
   */
  public void deregisterFromEvent(UUID userId, UUID eventId) {
    IStudent student = getStudent(userId);
    eventService.getEvent(eventId); // Validate that the event actually exists
    Set<UUID> studentEvents = student.getRegisteredEvents();

    // Ensure that the student was already signed up for the event
    boolean successfullyRemoved = studentEvents.remove(eventId);
    if (!successfullyRemoved) {
      throw new IllegalArgumentException("The student is not registered for the given event");
    }

    // Update the student's record
    student.setRegisteredEvents(studentEvents);
    userRepository.save(student);
    subscriptionService.unsubscribe(userId, eventId, SubscriptionSource.REGISTRATION);
  }

  /**
   * Get the set of events that a Student is registered for.
   *
   * @param userId the student's user ID
   * @return the set of events that the user is registered for; may be empty, never null
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   */
  public Set<IEvent> getEventsForStudent(UUID userId) {
    IStudent student = getStudent(userId);

    /*
     * Get a stream of event IDs and map them onto their actual entities. If an event ID points
     * to an event which does not exist, throw an error.
     */
    Set<UUID> events = student.getRegisteredEvents();
    Set<IEvent> studentEvents = new HashSet<>(
        events.stream()
        .map(eventId -> eventRepository.findByID(eventId)
            .orElseThrow(() -> new IllegalStateException("The given student is registered for an"
                + " event with ID " + eventId + ", which could not be found within the event"
                + " repository.")))
        .toList()
    );
    return studentEvents;
  }

  /**
   * Subscribe a Student to receive an Event's notifications without registering them to attend.
   *
   * @param userId the Student's user ID
   * @param eventId the Event's ID
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   * @throws EventNotFoundException if the event does not exist.
   * @throws IllegalArgumentException if the user has already subscribed to the event.
   */
  public void subscribeToEvent(UUID userId, UUID eventId) {
    getStudent(userId);
    eventService.getEvent(eventId);
    subscriptionService.subscribe(userId, eventId, SubscriptionSource.EXPLICIT);
  }

  /**
   * Unsubscribe a Student from an Event's notifications, without deregistering them.
   *
   * @param userId the Student's user ID
   * @param eventId the Event's ID
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   * @throws EventNotFoundException if the event does not exist.
   * @throws IllegalArgumentException if the user is not subscribed to the event.
   */
  public void unsubscribeFromEvent(UUID userId, UUID eventId) {
    getStudent(userId);
    eventService.getEvent(eventId);
    subscriptionService.unsubscribe(userId, eventId, SubscriptionSource.EXPLICIT);
  }

  // Utility Methods //

  /**
   * Retrieves a student from the injected user repository.
   *
   * @param userId the student's user ID
   * @return the {@code IStudent} entity; never null
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   */
  private IStudent getStudent(UUID userId) {
    // Verify that the user exists within the repository
    Optional<IUser> optionalUser = userRepository.findByID(userId);
    if (optionalUser.isEmpty()) {
      throw new UserNotFoundException("User does not exist");
    }

    // Verify that the user is a Student
    IUser user = optionalUser.get();
    if (user.getType() != UserType.STUDENT) {
      throw new UserNotAuthorisedException("The given user is not a student");
    }

    // Cast to a Student entity & return
    IStudent student = (IStudent) user;
    return student;
  }

  /**
   * Publishes a {@link NotificationType#CAPACITY_WARNING} when a registration brings the number of
   * registered students up to the warning threshold.
   *
   * <p>This is edge-triggered on the upward crossing, not latched. This means that an event whose
   * registered count oscillates around the threshold (through repeated deregister/register) emits
   * the warning on every crossing.
   *
   * <p>This method is a no-op for events with no capacity limit.
   *
   * @param eventId the ID of the event to target
   */
  private void publishCapacityWarningIfReached(UUID eventId) {
    Integer capacity = eventService.getEvent(eventId).getCapacity();
    if (capacity == null) {
      return;
    }
    int thresholdCount = (int) Math.ceil(capacity * CAPACITY_WARNING_THRESHOLD);
    if (countRegisteredStudents(eventId) == thresholdCount) {
      subscriptionService.publish(eventId, NotificationType.CAPACITY_WARNING);
    }
  }

  /** Counts the students currently registered for the given event. */
  private int countRegisteredStudents(UUID eventId) {
    Predicate<IUser> isUserStudent = user -> user.getType() == UserType.STUDENT;
    Predicate<IStudent> isStudentRegisteredForEvent =
        student -> student.getRegisteredEvents().contains(eventId);

    List<IUser> users = userRepository.findAll();
    return (int) users.stream()
        .filter(isUserStudent)
        .map(user -> (IStudent) user)
        .filter(isStudentRegisteredForEvent)
        .count();
  }

  /**
   * Determines whether a given event is full.
   *
   * @param eventId the event's ID
   * @return {@code true} if the event is full; {@code false} if there is still space remaining
   * @throws EventNotFoundException if the event does not exist
   */
  private boolean isEventFull(UUID eventId) {
    Integer capacity = eventService.getEvent(eventId).getCapacity();
    if (capacity == null) { // unlimited events are never full
      return false;
    }

    int numStudentsRegistered = countRegisteredStudents(eventId);
    if (numStudentsRegistered > capacity) {
      throw new IllegalStateException("The given event has too many students registered."
          + "\n- " + numStudentsRegistered + " students registered to attend"
          + "\n- Event has a maximum capacity of " + capacity);
    }

    return numStudentsRegistered == capacity;
  }

}
