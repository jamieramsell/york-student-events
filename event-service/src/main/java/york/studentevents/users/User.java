package york.studentevents.users;

import java.util.List;
import java.util.UUID;

public class User implements IUser{
    private final UUID id;
    private String username;
    private String email;
    private long cohort;
    private List<UUID> registeredEvents;

    /** Creates a {@code User} with the given details.
     * <p>
     * @param username the user's username; must not be {@code null} or blank
     * @param email the user's email; must not be {@code null} or blank
     * @param cohort the user's cohort; no validation is performed
     * @param registeredEvents the user's registered events; no validation is performed
     * <p>
     * @throws IllegalArgumentException if the username or email is invalid
     */
    public User(String username, String email, long cohort, List<UUID> registeredEvents) throws IllegalArgumentException {
        this.id = UUID.randomUUID();
        setUsername(username);
        setEmail(email);
        setCohort(cohort);
        setRegisteredEvents(registeredEvents);
    }

    /**
     * Returns the unique identifier of the user
     *
     * @return the user's ID
     */
    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Returns the user's username
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username
     *
     * @param username the new username; must not be {@code null} or blank
     * @throws IllegalArgumentException if the username is invalid
     */
    @Override
    public void setUsername(String username) throws IllegalArgumentException {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username cannot be null or empty.");
        this.username = username;
    }

    /**
     * Returns the user's password
     */
    @Override
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email
     *
     * @param email the new email; must not be {@code null} or blank
     * @throws IllegalArgumentException if the email is invalid
     */
    @Override
    public void setEmail(String email) throws IllegalArgumentException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email cannot be null or empty.");
        this.email = email;
    }

    /**
     * Returns the user's cohort
     */
    @Override
    public long getCohort() {
        return cohort;
    }

    /**
     * Sets the user's cohort
     *
     * @param cohort the new cohort; currently no validation is performed
     */
    @Override
    public void setCohort(long cohort) {
        this.cohort = cohort;
    }

    /**
     * Returns a list of events that the user has signed up to as a list of event IDs
     *
     * @return List of eventIds
     */
    @Override
    public List<UUID> getRegisteredEvents() {
        return registeredEvents;
    }

    /**
     * Sets the user's registered events
     *
     * @param events the new list of events; currently no validation is performed
     */
    @Override
    public void setRegisteredEvents(List<UUID> events) {
        this.registeredEvents = events;
    }

    /** Returns a string representation for debugging purposes. */
    @Override
    public String toString() {
        return String.format(
                "User[id=%s, username='%s', email='%s', cohort=%d, registeredEvents=%s]",
                id,
                username,
                email,
                cohort,
                registeredEvents
        );
    }
}
