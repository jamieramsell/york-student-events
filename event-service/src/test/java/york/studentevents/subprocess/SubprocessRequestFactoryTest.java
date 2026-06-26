package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubprocessRequestFactoryTest {

  private static final UUID USER_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static JsonObject parse(String json) {
    return JsonParser.parseString(json).getAsJsonObject();
  }

  @Test
  void buildGetUserBadgesProducesEnvelope() {
    JsonObject envelope = parse(SubprocessRequestFactory.buildGetUserBadges(USER_ID));
    assertEquals("GET_USER_BADGES", envelope.get("requestType").getAsString());
    assertEquals(
        USER_ID.toString(), envelope.getAsJsonObject("payload").get("userId").getAsString());
  }

  @Test
  void buildGetUserFriendsProducesEnvelope() {
    JsonObject envelope = parse(SubprocessRequestFactory.buildGetUserFriends(USER_ID));
    assertEquals("GET_USER_FRIENDS", envelope.get("requestType").getAsString());
    assertEquals(
        USER_ID.toString(), envelope.getAsJsonObject("payload").get("userId").getAsString());
  }

  @Test
  void buildAwardBadgeIncludesUserIdAndBadgeName() {
    JsonObject envelope = parse(SubprocessRequestFactory.buildAwardBadge(USER_ID, "Social5"));
    assertEquals("AWARD_BADGE", envelope.get("requestType").getAsString());
    JsonObject payload = envelope.getAsJsonObject("payload");
    assertEquals(USER_ID.toString(), payload.get("userId").getAsString());
    assertEquals("Social5", payload.get("badgeName").getAsString());
  }
}
