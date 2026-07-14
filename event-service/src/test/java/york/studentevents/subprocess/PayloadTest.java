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
  void awardBadgePayloadExposesUserIdAndBadgeName() {
    UUID id = UUID.randomUUID();
    AwardBadgePayload payload = new AwardBadgePayload(id, "First Event");
    assertEquals(id, payload.userId());
    assertEquals("First Event", payload.badgeName());
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
    UUID id = UUID.randomUUID();
    IPayload userIdPayload = new UserIdPayload(id);
    IPayload awardBadgePayload = new AwardBadgePayload(id, "Social5");
    IPayload attendancePayload = new AttendancePayload(id, UUID.randomUUID());
    assertEquals(id, userIdPayload.userId());
    assertEquals(id, awardBadgePayload.userId());
    assertEquals(id, attendancePayload.userId());
  }
}
