package york.studentevents.users;

import java.util.List;
import java.util.UUID;

/**
 * Represents a Student user of the platform, including their profile details, cohort, and
 * registered events.
 */
public class Student extends User implements IStudent {
  
  private UUID cohort;

  /** Creates a {@code User} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param cohort the user's cohort; no validation is performed
   * @param registeredEvents the user's registered events; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  public Student(String username, String email, UUID cohort, List<UUID> registeredEvents) {
    this(UUID.randomUUID(), username, email, cohort, registeredEvents);
  }

  /** Creates a {@code User} with the given details.
   *
   * @param id the user's ID; must not be {@code null}.
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param cohort the user's cohort; no validation is performed
   * @param registeredEvents the user's registered events; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  protected Student(UUID id, String username, String email, UUID cohort, List<UUID> registeredEvents) {
    super(id, username, email);
    setCohort(cohort);
    setRegisteredEvents(registeredEvents);
  }

  @Override
  public UUID getCohort() {
    return cohort;
  }

  @Override
  public void setCohort(UUID cohort) {
    this.cohort = cohort;
  }

  @Override
  public List<UUID> getRegisteredEvents() {
    return getEvents();
  }

  /**
   * Sets the user's registered events.
   *
   * @param events the new list of events; currently no validation is performed
   */
  @Override
  public void setRegisteredEvents(List<UUID> events) {
    setEvents(events);
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format(
        "User[id=%s, username='%s', email='%s', cohort=%s, registeredEvents=%s]",
        id,
        username,
        email,
        cohort,
        getEvents()
    );
  }
}