package york.studentevents.subprocess;

import java.util.UUID;

/**
 * Payload carrying a user ID and a badge name, for requests that award a badge to a user.
 *
 * @param userId the user's ID
 * @param badgeName the name of the badge to award
 */
record AwardBadgePayload(UUID userId, String badgeName) implements IPayload {}