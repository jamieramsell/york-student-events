package york.studentevents.users;

import java.util.Set;
import java.util.UUID;

/** Defines the contract for Host profiles, covering their profile data and hosting capability. */
public interface IHost extends IUser {
  
  /**
   * Returns a copy of the set of events that the user has announced that they will be hosting.
   *
   * @return a set of event IDs.
   */
  Set<UUID> getHostedEvents();

  /** Sets the user's hosted events.
   *
   * @param events the new set of events; currently no validation is performed
   */
  void setHostedEvents(Set<UUID> events);
}