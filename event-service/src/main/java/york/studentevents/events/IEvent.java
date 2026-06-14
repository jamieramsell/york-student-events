package york.studentevents.events;

import java.time.LocalDateTime;

/** Represents a social event that can be attended by students. */
public interface IEvent {

  /** Returns the unique identifier for this event. */
  long getId(); 
  
  String getTitle();
  
  String getDescription();
  
  LocalDateTime getDateTime();
  
  /** Returns the venue which is hosting the event. */
  String getLocation();
  
  /** Returns the maximum number of people who can attend the event. */
  int getCapacity();
  
  String getCategory();
  
}
