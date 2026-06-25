package york.studentevents.subprocess;

import java.util.UUID;

/**
 * Builds a request for the subprocess where the payload is a user ID and a badge name.
 *
 * @param userId the user's ID
 * @param badgeName the name of the badge to award
 */
record AwardBadgePayload(UUID userId, String badgeName) implements IPayload {}