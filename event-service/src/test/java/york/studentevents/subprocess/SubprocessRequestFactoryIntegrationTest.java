package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link SubprocessRequestFactory#sendRequest(String)}.
 *
 * <p>These spawn the real Python responder ({@code bridge/responder.py}) and assert on the JSON it
 * returns, exercising the Java-to-Python direction of the bridge end to end. The responder
 * currently returns canned stub data. Skipped when {@code python3} is not available; the script is
 * located via the {@code project.root} property set by surefire.
 */
class SubprocessRequestFactoryIntegrationTest {

  private static final UUID USER_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  // The pair pre-seeded into the responder's canned attendance repository
  // (InMemoryCannedAttendanceRepository); USER_ID doubles as the canned
  // attendee, so re-recording it against CANNED_EVENT_ID is a known duplicate.
  private static final UUID CANNED_EVENT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  @BeforeEach
  void requirePython3() {
    assumeTrue(python3Available(), "python3 is not available on PATH");
  }

  private static boolean python3Available() {
    try {
      Process process = new ProcessBuilder("python3", "--version").start();
      return process.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private static JsonObject send(String requestJson) {
    String response = SubprocessRequestFactory.sendRequest(requestJson);
    return JsonParser.parseString(response).getAsJsonObject();
  }

  @Test
  void getUserBadgesReturnsBadgesInOkEnvelope() {
    JsonObject response = send(SubprocessRequestFactory.buildGetUserBadges(USER_ID));
    assertEquals("ok", response.get("status").getAsString());
    JsonArray badges = response.getAsJsonObject("payload").getAsJsonArray("badges");
    assertEquals(2, badges.size());
    assertEquals("First Event", badges.get(0).getAsString());
    assertEquals("Social5", badges.get(1).getAsString());
  }

  @Test
  void getUserFriendsReturnsFriendsInOkEnvelope() {
    JsonObject response = send(SubprocessRequestFactory.buildGetUserFriends(USER_ID));
    assertEquals("ok", response.get("status").getAsString());
    JsonArray friends = response.getAsJsonObject("payload").getAsJsonArray("friends");
    assertEquals(2, friends.size());
    assertEquals("James", friends.get(0).getAsString());
    assertEquals("Jamie", friends.get(1).getAsString());
  }

  @Test
  void awardBadgeFailsAndSurfacesResponderError() {
    RuntimeException exception = assertThrows(
        RuntimeException.class,
        () -> SubprocessRequestFactory.sendRequest(
            SubprocessRequestFactory.buildAwardBadge(USER_ID, "Social5")));
    assertTrue(exception.getMessage().contains("THIS IS A TEST ERROR"));
  }

  @Test
  void recordAttendanceForNewPairReturnsOkWithEmptyPayload() {
    JsonObject response = send(
        SubprocessRequestFactory.buildRecordAttendance(USER_ID, UUID.randomUUID()));
    assertEquals("ok", response.get("status").getAsString());
    assertEquals(0, response.getAsJsonObject("payload").size());
  }

  @Test
  void recordAttendanceOfCannedRecordSurfacesDuplicateError() {
    RuntimeException exception = assertThrows(
        RuntimeException.class,
        () -> SubprocessRequestFactory.sendRequest(
            SubprocessRequestFactory.buildRecordAttendance(USER_ID, CANNED_EVENT_ID)));
    assertTrue(exception.getMessage().contains("already been recorded"));
  }
}
