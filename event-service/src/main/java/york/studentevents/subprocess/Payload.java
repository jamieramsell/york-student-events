package york.studentevents.subprocess;

import java.util.UUID;

sealed interface Payload permits UserIdPayload, AwardBadgePayload {
  public UUID userId();
}

/**
 * Builds a request for the subprocess where the payload is only a user ID.
 *
 * @param userId the user's ID
 */
record UserIdPayload(UUID userId) implements Payload {}

/**
 * Builds a request for the subprocess where the payload is a user ID and a badge name.
 *
 * @param userId the user's ID
 * @param badgeName the name of the badge to award
 */
record AwardBadgePayload(UUID userId, String badgeName) implements Payload {}