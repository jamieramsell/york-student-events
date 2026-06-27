package york.studentevents.users;

import york.studentevents.repository.IEntity;

/** Defines the core contract for user covering their profile data and relationships. */
public interface IUser extends IEntity {
  
  /** Returns the user's username. */
  String getUsername();

  /** Sets the user's username.
   *
   * @param username the new username; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the username is invalid.
   */
  void setUsername(String username);
  
  /** Returns the user's password. */
  String getEmail();

  /** Sets the user's email.
   *
   * @param email the new email; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the email is invalid.
   */
  void setEmail(String email);

}
