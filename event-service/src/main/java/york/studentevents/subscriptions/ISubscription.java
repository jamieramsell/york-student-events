package york.studentevents.subscriptions;

import java.util.UUID;
import york.studentevents.repository.IEntity;

/** Represents a Subscription, where a given user has subscribed to a given event. */
public interface ISubscription extends IEntity {

  /** Represents the reason why a given subscription instance was created. */
  enum SubscriptionSource {

    /** The subscription was created automatically, due to the user registering for the event. */
    REGISTRATION,

    /** The subscription was created manually by the user.*/
    EXPLICIT

  }

  /** Retrieves the ID of the User that the subscription targets. */
  UUID getUserId();

  /** Retrieves the ID of the Event that the subscription targets. */
  UUID getEventId();

  /** Retrieves the reason why the subscription was generated. */
  SubscriptionSource getSource();

}
