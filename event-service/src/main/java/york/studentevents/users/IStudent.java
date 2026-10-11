package york.studentevents.users;

import java.util.Set;
import java.util.UUID;

/**
 * Defines the core contract for Student profile, covering their profile data and relationships.
 *
 * <p>Sits between {@link User} and {@link Student} so that the student-specific contract is a
 * class, while still inheriting the shared profile state and persistence mapping from
 * {@link User}.
 */
public abstract class IStudent extends User {

  /** Creates a student contract with the given details. See {@link User#User(UUID, String,
   * String, String)}.
   */
  protected IStudent(UUID id, String username, String email, String passwordHash) {
    super(id, username, email, passwordHash);
  }

  /** No-args constructor for JPA use only. */
  protected IStudent() {}

  /** Returns a copy of the set of events that the user has signed up to, as a set of event IDs. */
  public abstract Set<UUID> getRegisteredEvents();

  /** Sets the user's registered events.
   *
   * @param events the new set of events; currently no validation is performed
   */
  public abstract void setRegisteredEvents(Set<UUID> events);
}
