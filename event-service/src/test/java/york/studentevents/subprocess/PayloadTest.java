package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayloadTest {

  @Test
  void userIdPayloadExposesUserId() {
    UUID id = UUID.randomUUID();
    assertEquals(id, new UserIdPayload(id).userId());
  }

  @Test
  void awardBadgePayloadExposesUserIdAndBadgeId() {
    UUID userId = UUID.randomUUID();
    UUID badgeId = UUID.randomUUID();
    AwardBadgePayload payload = new AwardBadgePayload(userId, badgeId);
    assertEquals(userId, payload.userId());
    assertEquals(badgeId, payload.badgeId());
  }

  @Test
  void attendancePayloadExposesUserIdAndEventId() {
    UUID userId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    AttendancePayload payload = new AttendancePayload(userId, eventId);
    assertEquals(userId, payload.userId());
    assertEquals(eventId, payload.eventId());
  }

  @Test
  void payloadsExposeUserIdViaInterface() {
    UUID userId = UUID.randomUUID();
    IPayload userIdPayload = new UserIdPayload(userId);
    IPayload awardBadgePayload = new AwardBadgePayload(userId, UUID.randomUUID());
    IPayload attendancePayload = new AttendancePayload(userId, UUID.randomUUID());
    assertEquals(userId, userIdPayload.userId());
    assertEquals(userId, awardBadgePayload.userId());
    assertEquals(userId, attendancePayload.userId());
  }
}
