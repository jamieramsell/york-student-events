package york.studentevents.users;

import java.util.List;
import java.util.UUID;

/** Represents a Host user of the platform, including their profile details, and hosted events. */
public class Host extends User implements IHost {

  /** Creates a {@code Host} with the given details.
   *
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param cohort the user's cohort; no validation is performed
   * @param hostedEvents the events hosted by the user; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  public Host(String username, String email, List<UUID> hostedEvents) {
    this(UUID.randomUUID(), username, email, hostedEvents);
  }

  /** Creates a {@code Host} with the given details.
   *
   * @param id the user's ID; must not be {@code null}.
   * @param username the user's username; must not be {@code null}, blank, or empty.
   * @param email the user's email; must not be {@code null}, blank, or empty.
   * @param hostedEvents the events hosted by the user; no validation is performed
   * @throws IllegalArgumentException if the username or email is invalid
   */
  protected Host(UUID id, String username, String email, List<UUID> hostedEvents) {
    super(id, username, email);
    setHostedEvents(hostedEvents);
  }

  @Override
  public List<UUID> getHostedEvents() {
    return getEvents();
  }

  @Override
  public void setHostedEvents(List<UUID> events) {
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
        "Host[id=%s, username='%s', email='%s', hostedEvents=%s]",
        id,
        username,
        email,
        getEvents()
    );
  }
}