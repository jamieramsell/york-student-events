package york.studentevents.users;

import java.util.List;
import java.util.UUID;

/** Defines the core contract for Student profile, covering their profile data and relationships. */
public interface IStudent extends IUser {

  /** Returns the user's cohort. */
  UUID getCohort();

  /** Sets the user's cohort.
   *
   * @param cohort the new cohort; currently no validation is performed
   */
  void setCohort(UUID cohort);
  
  /** Returns a copy of the list of events that the user has signed up to as a list of event IDs. */
  List<UUID> getRegisteredEvents();

  /** Sets the user's registered events.
   *
   * @param events the new list of events; currently no validation is performed
   */
  void setRegisteredEvents(List<UUID> events);
}