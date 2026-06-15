package york.studentevents.users;
import java.util.List;
/** Defines the core contract for user covering their profile data and relationships */
public interface IUser{
    
    /** Returns the unique identifier of the user
     * @return the user's ID
     */
    long getId();
    
    String getUsername();
    
    String getEmail();
    
    long getCohort();
    
    /** Returns a list of events user signed up to as list of Long eventIds
     * @return List of Long eventIds
     */
    List<Long> getRegisteredEvents();
}