package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

  private String previousProjectRoot;

  @BeforeEach
  void setUp() {
    previousProjectRoot = System.getProperty("project.root");
    assumeTrue(python3Available(), "python3 is not available on PATH");
    // Anchor the bridge script to the repo root regardless of launch context: Maven's surefire
    // sets project.root, but an IDE running JUnit directly leaves user.dir at the module folder.
    System.setProperty("project.root", repoRoot().toString());
  }

  @AfterEach
  void restoreProjectRoot() {
    if (previousProjectRoot == null) {
      System.clearProperty("project.root");
    } else {
      System.setProperty("project.root", previousProjectRoot);
    }
  }

  private static Path repoRoot() {
    Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (dir != null && !Files.exists(dir.resolve("api-core/src/bridge/responder.py"))) {
      dir = dir.getParent();
    }
    if (dir == null) {
      throw new IllegalStateException(
          "Could not locate the repository root from " + System.getProperty("user.dir"));
    }
    return dir;
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
  void awardBadgeReturnsErrorEnvelope() {
    JsonObject response = send(SubprocessRequestFactory.buildAwardBadge(USER_ID, "Social5"));
    assertEquals("error", response.get("status").getAsString());
    assertTrue(response.has("error"));
  }
}
