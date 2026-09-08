package york.studentevents.subprocess;

import java.util.UUID;

/**
 * Payload carrying a User ID and a Badge ID, for requests that award a Badge to a User.
 *
 * @param userId the User's ID
 * @param badgeId the ID of the Badge to award
 */
record AwardBadgePayload(UUID userId, UUID badgeId) implements IPayload {}
