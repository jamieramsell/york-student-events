package york.studentevents.users;

import java.util.List;
import java.util.UUID;

import york.studentevents.repository.IEntity;

/** Defines the core contract for user covering their profile data and relationships */
public interface IUser extends IEntity{
    
    String getUsername();
    
    String getEmail();
    
    UUID getCohort();
    
    /** Returns a list of events that the user has signed up to as list of event IDs
     * 
     * @return List of eventIds
     */
    List<UUID> getRegisteredEvents();
}
