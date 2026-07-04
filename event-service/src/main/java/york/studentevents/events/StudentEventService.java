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
  
  private final IEventRepository eventRepository;
  private final IUserRepository userRepository;

  /**
   * Constructor for UserEventService.
   *
   * @param eventRepository the event repository which events are registered to
   * @param userRepository the user repository which users are registered to
   */
  public StudentEventService(IEventRepository eventRepository, IUserRepository userRepository) {
    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
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
  void registerForEvent(UUID userId, UUID eventId) {
    IStudent student = getStudent(userId);

    if (isEventFull(eventId)) {
      throw new CapacityExceededException();
    }

    Set<UUID> studentEvents = student.getRegisteredEvents();

    // Ensure that the student was already signed up for the event
    boolean successfullyAdded = studentEvents.add(eventId);
    if (!successfullyAdded) {
      throw new IllegalArgumentException("The student is already registered for this event");
    }

    // Update the student's record
    student.setRegisteredEvents(studentEvents);
    userRepository.save(student);
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
  void deregisterFromEvent(UUID userId, UUID eventId) {
    IStudent student = getStudent(userId);
    getEvent(eventId); // Validate that the event actually exists
    Set<UUID> studentEvents = student.getRegisteredEvents();

    // Ensure that the student was already signed up for the event
    boolean successfullyRemoved = studentEvents.remove(eventId);
    if (!successfullyRemoved) {
      throw new IllegalArgumentException("The student is not registered for the given event");
    }

    // Update the student's record
    student.setRegisteredEvents(studentEvents);
    userRepository.save(student);
  }

  /**
   * Get the set of events that a Student is registered for.
   *
   * @param userId the student's user ID
   * @return the set of events that the user is registered for; may be empty, never null
   * @throws UserNotFoundException if the user does not exist
   * @throws UserNotAuthorisedException if the given user is not a Student
   */
  Set<IEvent> getEventsForStudent(UUID userId) {
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
   * Retrieves a student from the injected user repository.
   *
   * @param eventId the event's ID
   * @return the {@code IEvent} entity; never null
   * @throws EventNotFoundException if the event does not exist
   */
  private IEvent getEvent(UUID eventId) {
    Optional<IEvent> optionalEvent = eventRepository.findByID(eventId);
    if (optionalEvent.isEmpty()) {
      throw new EventNotFoundException();
    }
    return optionalEvent.get();
  }

  /**
   * Determines whether a given event is full.
   *
   * @param eventId the event's ID
   * @return {@code true} if the event is full; {@code false} if there is still space remaining
   * @throws EventNotFoundException if the event does not exist
   */
  private boolean isEventFull(UUID eventId) {
    IEvent event = getEvent(eventId);

    // Create anonymous functions required to find the number of students registered for the event
    Predicate<IUser> isUserStudent = user -> user.getType() == UserType.STUDENT;
    Predicate<IStudent> isStudentRegisteredForEvent = student ->
        student.getRegisteredEvents().contains(eventId);

    // Retrieve all students registered for the event
    List<IUser> users = userRepository.findAll();
    Set<IStudent> registeredStudents = new HashSet<>(
        users.stream()
        .filter(isUserStudent)
        .map(user -> (IStudent) user)
        .filter(isStudentRegisteredForEvent)
        .toList()
    );

    // Validate that the number of students attending has not exceeded the max capacity of the event
    int numStudentsRegistered = registeredStudents.size();
    int eventCapacity = event.getCapacity();

    if (numStudentsRegistered > eventCapacity) {
      throw new IllegalStateException("The given event has too many students registered."
          + "\n- " + numStudentsRegistered + " students registered to attend"
          + "\n- Event has a maximum capacity of " + eventCapacity
      );
    } 

    // Return whether the event is full
    return numStudentsRegistered == eventCapacity;
  }

}
