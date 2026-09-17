package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link SubprocessRequestFactory#sendRequest(String)}.
 *
 * <p>These spawn the real Python responder ({@code bridge/responder.py}) and assert on the JSON it
 * returns, exercising the Java-to-Python direction of the bridge end to end. They cover the wire
 * contract — the {@code ok} / empty-payload / {@code error} envelope shapes — rather than persisted
 * data: the responder is spawned with the {@code YSE_BRIDGE_INMEMORY} flag (set for the surefire
 * fork in the pom and inherited by the spawned process), so it composes an empty in-memory backend
 * and needs no database. The seeded-data round-trip is covered on the Python side in
 * {@code test_bridge_responder.py}. Skipped when {@code python} is not available; the script is
 * located via the {@code project.root} property set by surefire.
 */
class SubprocessRequestFactoryIntegrationTest {

  // A sample user id; the in-memory backend holds no data for it, so read
  // handlers return empty collections.
  private static final UUID USER_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  @BeforeEach
  void requirePython() {
    assumeTrue(pythonAvailable(), "python is not available on PATH");
  }

  private static boolean pythonAvailable() {
    try {
      Process process = new ProcessBuilder("python", "--version").start();
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
  void getUserBadgesReturnsEmptyBadgesInOkEnvelope() {
    JsonObject response = send(SubprocessRequestFactory.buildGetUserBadges(USER_ID));
    assertEquals("ok", response.get("status").getAsString());
    assertEquals(0, response.getAsJsonObject("payload").getAsJsonArray("badges").size());
  }

  @Test
  void getUserFriendsReturnsEmptyFriendsInOkEnvelope() {
    JsonObject response = send(SubprocessRequestFactory.buildGetUserFriends(USER_ID));
    assertEquals("ok", response.get("status").getAsString());
    assertEquals(0, response.getAsJsonObject("payload").getAsJsonArray("friends").size());
  }

  @Test
  void awardBadgeForUnknownBadgeSurfacesResponderError() {
    // No badge exists in the empty backend, so the responder rejects the award and
    // the non-zero exit is surfaced as a RuntimeException by sendRequest.
    RuntimeException exception = assertThrows(
        RuntimeException.class,
        () -> SubprocessRequestFactory.sendRequest(
            SubprocessRequestFactory.buildAwardBadge(USER_ID, UUID.randomUUID())));
    assertTrue(exception.getMessage().contains("Subprocess failed"));
  }

  @Test
  void recordAttendanceForNewPairReturnsOkWithEmptyPayload() {
    JsonObject response = send(
        SubprocessRequestFactory.buildRecordAttendance(USER_ID, UUID.randomUUID()));
    assertEquals("ok", response.get("status").getAsString());
    assertEquals(0, response.getAsJsonObject("payload").size());
  }

  @Test
  void getRecommendedFriendsReturnsEmptyListInOkEnvelope() {
    JsonObject response = send(
        SubprocessRequestFactory.buildGetRecommendedFriends(USER_ID));
    assertEquals("ok", response.get("status").getAsString());
    assertEquals(0, response.getAsJsonObject("payload").getAsJsonArray("friends").size());
  }
}
