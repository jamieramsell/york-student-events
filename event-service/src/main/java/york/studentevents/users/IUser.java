package york.studentevents.users;

import java.util.List;
import java.util.UUID;
import york.studentevents.repository.IEntity;

/** Defines the core contract for user covering their profile data and relationships. */
public interface IUser extends IEntity {
  
  String getUsername();

  /** Sets the user's username.
   *
   * @param username the new username; must not be {@code null} or blank
   * @throws IllegalArgumentException if the username is invalid
   */
  void setUsername(String username);
  
  /** Returns the user's password. */
  String getEmail();

  /** Sets the user's email.
   *
   * @param email the new email; must not be {@code null} or blank
   * @throws IllegalArgumentException if the email is invalid
   */
  void setEmail(String email);

  /** Returns the user's cohort. */
  UUID getCohort();

  /** Sets the user's cohort.
   *
   * @param cohort the new cohort; currently no validation is performed
   */
  void setCohort(UUID cohort);
  
  /** Returns a list of events that the user has signed up to as a list of event IDs. */
  List<UUID> getRegisteredEvents();

  /** Sets the user's registered events.
   *
   * @param events the new list of events; currently no validation is performed
   */
  void setRegisteredEvents(List<UUID> events);
}
