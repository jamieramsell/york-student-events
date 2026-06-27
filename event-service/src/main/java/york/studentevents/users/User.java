package york.studentevents.users;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Represents a user of the platform */
public abstract class User implements IUser {
  
  private final UUID id;
  private String username;
  private String email;
  private List<UUID> events = new ArrayList<>(); // storage only, no public accessor

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

  /**
   * Returns a copy of the shared list of event IDs backing this user. The meaning of these events
   * is defined by the concrete subtype (registered (attended) events for a {@code Student}, hosted
   * events for a {@code Host}) which is why this accessor is {@code protected} and exposed publicly
   * only under a role-specific name.
   * 
   * @return a copy of the list of associated event IDs; never {@code null}
   */
  protected List<UUID> getEvents() {
    return new ArrayList<>(events);
  }

  /**
   * Replaces this user's list of associated event IDs. The semantics of the list are determined by
   * the concrete subtype.
   *
   * @param events the new list of event IDs; must not be null.
   * 
   * @throws IllegalArgumentException if {@code events} is null.
   */
  protected void setEvents(List<UUID> events) {
    if (events == null) {
      throw new IllegalArgumentException("events cannot be null.");
    }
    this.events = events;
  }

}
