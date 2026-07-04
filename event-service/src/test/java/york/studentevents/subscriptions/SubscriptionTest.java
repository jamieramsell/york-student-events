package york.studentevents.subscriptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static york.studentevents.subscriptions.ISubscription.SubscriptionSource.REGISTRATION;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tests the {@link Subscription} entity: construction, validation, and identity. */
class SubscriptionTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();

  @Test
  void constructor_withValidArguments_assignsFieldsAndGeneratesId() {
    Subscription subscription = new Subscription(USER_ID, EVENT_ID, REGISTRATION);

    assertEquals(USER_ID, subscription.getUserId());
    assertEquals(EVENT_ID, subscription.getEventId());
    assertEquals(REGISTRATION, subscription.getSource());
    assertNotNull(subscription.getId());
  }

  @Test
  void constructor_withNullUserId_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Subscription(null, EVENT_ID, REGISTRATION));
  }

  @Test
  void constructor_withNullEventId_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Subscription(USER_ID, null, REGISTRATION));
  }

  @Test
  void constructor_withNullSource_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Subscription(USER_ID, EVENT_ID, null));
  }

  @Test
  void constructor_withNullId_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Subscription(null, USER_ID, EVENT_ID, REGISTRATION));
  }

  @Test
  void getId_twoSubscriptions_haveDistinctIds() {
    Subscription first = new Subscription(USER_ID, EVENT_ID, REGISTRATION);
    Subscription second = new Subscription(USER_ID, EVENT_ID, REGISTRATION);
    assertNotEquals(first.getId(), second.getId());
  }
}