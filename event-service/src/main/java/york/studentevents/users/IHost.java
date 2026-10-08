package york.studentevents.users;

import java.util.Set;
import java.util.UUID;

/**
 * Defines the contract for Host profiles, covering their profile data and hosting capability.
 *
 * <p>Sits between {@link User} and {@link Host} so that the host-specific contract is a class,
 * while still inheriting the shared profile state and persistence mapping from {@link User}.
 */
public abstract class IHost extends User {

  /** Creates a host contract with the given details. See {@link User#User(UUID, String, String,
   * String)}.
   */
  protected IHost(UUID id, String username, String email, String passwordHash) {
    super(id, username, email, passwordHash);
  }

  /** No-args constructor for JPA use only. */
  protected IHost() {}

  /**
   * Returns a copy of the set of events that the user has announced that they will be hosting.
   *
   * @return a set of event IDs.
   */
  public abstract Set<UUID> getHostedEvents();

  /** Sets the user's hosted events.
   *
   * @param events the new set of events; currently no validation is performed
   */
  public abstract void setHostedEvents(Set<UUID> events);
}
