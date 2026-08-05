package york.studentevents.users;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Student user of the platform, including their profile details, cohort, and
 * registered events.
 */
@Entity
@DiscriminatorValue("STUDENT")
public class Student extends User implements IStudent {
  
  /** Creates a {@code Student} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param passwordHash the user's password hash; must not be {@code null}, blank, or empty.
   * @param registeredEvents the user's registered events; no validation is performed
   * @throws IllegalArgumentException if the username, email, or passwordHash is invalid
   */
  public Student(
      String username,
      String email,
      String passwordHash, 
      Set<UUID> registeredEvents
  ) {
    this(UUID.randomUUID(), username, email, passwordHash, registeredEvents);
  }

  /** Creates a {@code Student} with the given details.
   *
   * @param id the user's ID; must not be {@code null}.
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param passwordHash the user's password hash; must not be {@code null}, blank, or empty.
   * @param registeredEvents the user's registered events; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  protected Student(UUID id, 
      String username, 
      String email, 
      String passwordHash,
      Set<UUID> registeredEvents
  ) {
    super(id, username, email, passwordHash);
    setRegisteredEvents(registeredEvents);
  }

  @Override
  public Set<UUID> getRegisteredEvents() {
    return getEvents();
  }

  @Override
  public void setRegisteredEvents(Set<UUID> events) {
    setEvents(events);
  }

  @Override
  public UserType getType() {
    return UserType.STUDENT;
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format(
        "Student[id=%s, username='%s', email='%s', cohort=%s, registeredEvents=%s]",
        id,
        username,
        email,
        getEvents()
    );
  }
}