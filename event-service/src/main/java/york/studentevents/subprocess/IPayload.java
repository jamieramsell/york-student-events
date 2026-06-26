package york.studentevents.subprocess;

import java.util.UUID;

/**
 * The payload of a subprocess request.
 *
 * <p>A payload carries the arguments for a single {@link RequestType}. Every payload identifies a
 * target user, so {@code userId} is common to all implementations; request-specific fields are
 * added by the individual records. The hierarchy is sealed to the known payload shapes.
 *
 * @see UserIdPayload
 * @see AwardBadgePayload
 * @see RequestType
 */
sealed interface IPayload permits UserIdPayload, AwardBadgePayload {

  /** Returns the ID of the user this request targets. */
  UUID userId();
  
}
