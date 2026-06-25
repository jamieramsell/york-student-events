package york.studentevents.subprocess;

import java.util.UUID;

sealed interface IPayload permits UserIdPayload, AwardBadgePayload {
  public UUID userId();
}
