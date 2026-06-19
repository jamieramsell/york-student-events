package york.studentevents.users;

import java.util.List;
import java.util.UUID;

/**
 * Represents a user of the platform, including their profile details, cohort, and registered
 * events.
 */
public class User implements IUser {
  
  private final UUID id;
  private String username;
  private String email;
  private UUID cohort;
  private List<UUID> registeredEvents;

  /** Creates a {@code User} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param cohort the user's cohort; no validation is performed
   * @param registeredEvents the user's registered events; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  public User(String username, String email, UUID cohort, List<UUID> registeredEvents) {
    this.id = UUID.randomUUID();
    setUsername(username);
    setEmail(email);
    setCohort(cohort);
    setRegisteredEvents(registeredEvents);
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  /**
   * Sets the user's username.
   *
   * @param username the new username; must not be {@code null}, blank, or empty
   * @throws IllegalArgumentException if the username is invalid
   */
  @Override
  public void setUsername(String username) {
    if (username == null || username.isBlank() || username.isEmpty()) {
      throw new IllegalArgumentException("Username cannot be null, blank, or empty.");
    }
    this.username = username;
  }

  @Override
  public String getEmail() {
    return email;
  }

  /**
   * Sets the user's email.
   *
   * @param email the new email; must not be {@code null} or blank
   * @throws IllegalArgumentException if the email is invalid, null, empty, or blank.
   */
  @Override
  public void setEmail(String email) throws IllegalArgumentException {
    if (email == null || email.isBlank() || email.isEmpty()) {
      throw new IllegalArgumentException("email cannot be null, blank, or empty.");
    } else if (email.split("@").length != 2) {
      throw new IllegalArgumentException("email provided is not valid");
    }
    this.email = email;
  }

  @Override
  public UUID getCohort() {
    return cohort;
  }

  /**
   * Sets the user's cohort.
   *
   * @param cohort the new cohort; currently no validation is performed
   */
  @Override
  public void setCohort(UUID cohort) {
    this.cohort = cohort;
  }

  /**
   * Returns a list of events that the user has signed up to as a list of event IDs.
   *
   * @return List of eventIds
   */
  @Override
  public List<UUID> getRegisteredEvents() {
    return registeredEvents;
  }

  /**
   * Sets the user's registered events.
   *
   * @param events the new list of events; currently no validation is performed
   */
  @Override
  public void setRegisteredEvents(List<UUID> events) {
    this.registeredEvents = events;
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format(
        "User[id=%s, username='%s', email='%s', cohort=%d, registeredEvents=%s]",
        id,
        username,
        email,
        cohort,
        registeredEvents
    );
  }
}
