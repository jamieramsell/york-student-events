package york.studentevents.users;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Represents a user of the platform. */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User implements IUser {
  
  @Id
  protected UUID id;

  @Column(nullable = false)
  protected String username;

  @Column(nullable = false)
  protected String email;

  @Column(nullable = false)
  protected String passwordHash;

  @ElementCollection
  @CollectionTable(
      name = "user_events",
      joinColumns = @JoinColumn(name = "user_id")
  )
  @Column(name = "event_id")
  private Set<UUID> events = new HashSet<>(); // storage only, no public accessor

  /** Creates a {@code User} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param passwordHash the user's password hash; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if a given parameter is null, blank, or empty.
   */
  protected User(String username, String email, String passwordHash) {
    this(UUID.randomUUID(), username, email, passwordHash);
  }

  /** Creates a {@code User} with the given details.
   *
   * @param id the user's ID; must not be {@code null}.
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param passwordHash the user's password hash; must not be {@code null}, blank, or empty.
   * @throws IllegalArgumentException if a given parameter is null, blank, or empty.
   */
  protected User(UUID id, String username, String email, String passwordHash) {
    if (id == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    this.id = id;
    setUsername(username);
    setEmail(email);
    this.passwordHash = passwordHash;
  }

  /** No-args constructor for JPA use only. */
  protected User() {}

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
    if (username == null || username.isBlank() || username.isEmpty()) {
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
    if (email == null || email.isBlank() || email.isEmpty()) {
      throw new IllegalArgumentException("email cannot be null, blank, or empty.");
    } else if (email.split("@").length != 2) {
      throw new IllegalArgumentException("email provided is not valid");
    }
    this.email = email;
  }

  @Override
  public String getPasswordHash() {
    return passwordHash;
  }

  @Override
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
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
   * Returns a copy of the shared set of event IDs backing this user. The meaning of these events
   * is defined by the concrete subtype (registered (attended) events for a {@code Student}, hosted
   * events for a {@code Host}) which is why this accessor is {@code protected} and exposed publicly
   * only under a role-specific name.
   *
   * @return a copy of the set of associated event IDs; never {@code null}
   */
  protected Set<UUID> getEvents() {
    return new HashSet<>(events);
  }

  /**
   * Replaces this user's set of associated event IDs. The semantics of the set are determined by
   * the concrete subtype.
   *
   * @param events the new set of event IDs; must not be null.
   * 
   * @throws IllegalArgumentException if {@code events} is null.
   */
  protected void setEvents(Set<UUID> events) {
    if (events == null) {
      throw new IllegalArgumentException("events cannot be null.");
    }
    this.events = new HashSet<>(events);
  }

}
