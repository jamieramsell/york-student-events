package york.studentevents.subprocess;

import java.util.UUID;

/**
 * Payload carrying only a user ID, for requests that operate on a single user.
 *
 * @param userId the user's ID
 */
record UserIdPayload(UUID userId) implements IPayload {}