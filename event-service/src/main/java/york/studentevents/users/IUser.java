package york.studentevents.users;

import york.studentevents.repository.IEntity;

/** Defines the core contract for user covering their profile data and relationships. */
public interface IUser extends IEntity {
  
  /**
   * Identifies the role of a user, distinguishing the two account types that the platform supports.
   *
   * <p>Used as a discriminator so that callers can determine a user's role without relying on
   * {@code instanceof}.
   */
  enum UserType {

    /** A student account: attends events and participates in the social layer. */
    STUDENT,

    /** 
     * A host account: an organiser such as a society, the SU, a club, or a company that announces
     * events. 
     */
    HOST

  }

  /** Returns the user's username. */
  String getUsername();

  /** Sets the user's username.
   *
   * @param username the new username; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the username is invalid.
   */
  void setUsername(String username);
  
  /** Returns the user's email. */
  String getEmail();

  /** Sets the user's email.
   *
   * @param email the new email; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the email is invalid.
   */
  void setEmail(String email);

  /** Retrieves the user's password hash. */
  String getPasswordHash();

  /** Sets the user's password hash.
   *
   * @param hash the new password hash; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the hash is invalid.
   */
  void setPasswordHash(String hash);

  /**
   * Returns the role of this user.
   *
   * @return {@link UserType#STUDENT} for a student account or {@link UserType#HOST} for a host
   *     account; never {@code null}
   */
  UserType getType();

}
