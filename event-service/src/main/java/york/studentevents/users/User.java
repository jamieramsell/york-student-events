package york.studentevents.users;

import java.util.UUID;

/** Represents a user of the platform */
public abstract class User implements IUser {
  
  private final UUID id;
  private String username;
  private String email;

  /** Creates a {@code User} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the username or email is invalid
   */
  protected User(String username, String email) {
    this(UUID.randomUUID(), username, email);
  }

  /** Creates a {@code User} with the given details.
   *
   * @param id the user's ID; must not be {@code null}.
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if the username or email is invalid
   */
  protected User(UUID id, String username, String email) {
    if (id == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    this.id = id;
    setUsername(username);
    setEmail(email);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public void setUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username cannot be null, blank, or empty.");
    }
    this.username = username;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public void setEmail(String email) throws IllegalArgumentException {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email cannot be null, blank, or empty.");
    } else if (email.split("@").length != 2) {
      throw new IllegalArgumentException("email provided is not valid");
    }
    this.email = email;
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format(
        "User[id=%s, username='%s', email='%s']",
        id,
        username,
        email
    );
  }
}
