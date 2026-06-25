package york.studentevents.subprocess;

import java.util.UUID;

abstract class Payload {
  public final UUID userId;

  public Payload(UUID userId) {
    this.userId = userId;
  }

  public UUID userId() {
    return userId;
  }
}

/**
 * Builds a request for the subprocess where the payload is only a user ID.
 *
 * @param userId the user's ID
 */
class UserIdPayload extends Payload {
  public UserIdPayload(UUID userId) {
    super(userId);
  }
}

/**
 * Builds a request for the subprocess where the payload is a user ID and a badge name.
 *
 * @param userId the user's ID
 * @param badgeName the name of the badge to award
 */
class AwardBadgePayload extends Payload {
  private final String badgeName;

  public AwardBadgePayload(UUID userId, String badgeName) {
    super(userId);
    this.badgeName = badgeName;
  }

  public String badgeName() {
    return badgeName;
  }
}