package york.studentevents.users;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Set;
import java.util.UUID;

/** Represents a Host user of the platform, including their profile details, and hosted events. */
@Entity
@DiscriminatorValue("HOST")
public class Host extends User implements IHost {

  /** Creates a {@code Host} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param passwordHash the user's password hash; must not be {@code null}, blank, or empty.
   * @param hostedEvents the events hosted by the user; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  public Host(String username, String email, String passwordHash, Set<UUID> hostedEvents) {
    this(UUID.randomUUID(), username, email, passwordHash, hostedEvents);
  }

  /** Creates a {@code Host} with the given details.
   *
   * @param id the user's ID; must not be {@code null}.
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param passwordHash the user's password hash; must not be {@code null}, blank, or empty.
   * @param hostedEvents the events hosted by the user; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  protected Host(
      UUID id,
      String username,
      String email,
      String passwordHash,
      Set<UUID> hostedEvents
  ) {
    super(id, username, email, passwordHash);
    setHostedEvents(hostedEvents);
  }

  @Override
  public Set<UUID> getHostedEvents() {
    return getEvents();
  }

  @Override
  public void setHostedEvents(Set<UUID> events) {
    setEvents(events);
  }

  @Override
  public UserType getType() {
    return UserType.HOST;
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format(
        "Host[id=%s, username='%s', email='%s', passwordHash='%s' hostedEvents=%s]",
        id,
        username,
        email,
        passwordHash,
        getEvents()
    );
  }
}