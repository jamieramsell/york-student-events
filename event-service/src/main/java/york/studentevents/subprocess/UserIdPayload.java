package york.studentevents.subprocess;

import java.util.UUID;

/**
 * Builds a request for the subprocess where the payload is only a user ID.
 *
 * @param userId the user's ID
 */
record UserIdPayload(UUID userId) implements IPayload {}