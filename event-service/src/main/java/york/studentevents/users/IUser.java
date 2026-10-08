package york.studentevents.users;

import york.studentevents.repository.IEntity;

/** Defines the core contract for user covering their profile data and relationships. */
public abstract class IUser extends IEntity {
  
  /**
   * Identifies the role of a user, distinguishing the two account types that the platform supports.
   *
   * <p>Used as a discriminator so that callers can determine a user's role without relying on
   * {@code instanceof}.
   */
  public enum UserType {

    /** A student account: attends events and participates in the social layer. */
    STUDENT,

    /** 
     * A host account: an organiser such as a society, the SU, a club, or a company that announces
     * events. 
     */
    HOST

  }

  /** Returns the user's username. */
  public abstract String getUsername();

  /** Sets the user's username.
   *
   * @param username the new username; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the username is invalid.
   */
  public abstract void setUsername(String username);
  
  /** Returns the user's email. */
  public abstract String getEmail();

  /** Sets the user's email.
   *
   * @param email the new email; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the email is invalid.
   */
  public abstract void setEmail(String email);

  /** Retrieves the user's password hash. */
  public abstract String getPasswordHash();

  /** Sets the user's password hash.
   *
   * @param hash the new password hash; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the hash is invalid.
   */
  public abstract void setPasswordHash(String hash);

  /**
   * Returns the role of this user.
   *
   * @return {@link UserType#STUDENT} for a student account or {@link UserType#HOST} for a host
   *     account; never {@code null}
   */
  public abstract UserType getType();

}
