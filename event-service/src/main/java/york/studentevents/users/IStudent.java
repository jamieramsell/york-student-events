package york.studentevents.users;

import java.util.Set;
import java.util.UUID;

/** Defines the core contract for Student profile, covering their profile data and relationships. */
public interface IStudent extends IUser {
  
  /** Returns a copy of the set of events that the user has signed up to, as a set of event IDs. */
  Set<UUID> getRegisteredEvents();

  /** Sets the user's registered events.
   *
   * @param events the new set of events; currently no validation is performed
   */
  void setRegisteredEvents(Set<UUID> events);
}