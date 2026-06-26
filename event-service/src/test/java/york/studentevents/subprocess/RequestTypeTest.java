package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RequestTypeTest {

  @Test
  void getRequestReturnsWireString() {
    assertEquals("GET_USER_EVENTS", RequestType.GET_USER_EVENTS.getRequest());
    assertEquals("GET_USER_BADGES", RequestType.GET_USER_BADGES.getRequest());
    assertEquals("GET_USER_FRIENDS", RequestType.GET_USER_FRIENDS.getRequest());
    assertEquals("AWARD_BADGE", RequestType.AWARD_BADGE.getRequest());
  }

  @Test
  void wireStringMatchesEnumNameForEveryConstant() {
    for (RequestType type : RequestType.values()) {
      assertEquals(type.name(), type.getRequest());
      assertEquals(type, RequestType.valueOf(type.name()));
    }
  }
}
