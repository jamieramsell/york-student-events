package york.studentevents.users;

import java.util.List;
import java.util.UUID;

/** Defines the core contract for a user covering their profile data and relationships */
public interface IUser{
    
    /** Returns the unique identifier of the user
     * @return the user's ID
     */
    UUID getId();

    /** Returns the user's username*/
    String getUsername();

    /** Sets the user's username
     * @param username the new username; must not be {@code null} or blank
     * @throws IllegalArgumentException if the username is invalid
     */
    void setUsername(String username) throws IllegalArgumentException;
    
    /** Returns the user's password*/
    String getEmail();

    /** Sets the user's email
     * @param email the new email; must not be {@code null} or blank
     * @throws IllegalArgumentException if the email is invalid
     */
    void setEmail(String email) throws IllegalArgumentException;

    /** Returns the user's cohort*/
    UUID getCohort();

    /** Sets the user's cohort
     * @param cohort the new cohort; currently no validation is performed*/
    void setCohort(UUID cohort);
    
    /** Returns a list of events that the user has signed up to as a list of event IDs
     * @return List of eventIds
     */
    List<UUID> getRegisteredEvents();

    /** Sets the user's registered events
     * @param events the new list of events; currently no validation is performed*/
    void setRegisteredEvents(List<UUID> events);
}
