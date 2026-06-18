package york.studentevents.users;

import java.util.List;
import java.util.UUID;

/** Defines the core contract for user covering their profile data and relationships. */
public interface IUser {
  
  /** Returns the unique identifier of the user.
   *
   * @return the user's ID
   */
  long getId();
  
  String getUsername();
  
  String getEmail();
  
  long getCohort();
  
  /** Returns a list of events that the user has signed up to as list of event IDs.
   *
   * @return List of eventIds
   */
  List<UUID> getRegisteredEvents();
}
