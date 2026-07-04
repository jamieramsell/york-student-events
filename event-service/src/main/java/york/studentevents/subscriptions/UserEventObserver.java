package york.studentevents.subscriptions;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IObserver} representing a single user's interest in an event's notifications.
 *
 * <p>Adapts a platform user to the Observer pattern: it identifies the user to notify and, when
 * notified, delivers the notification.
 *
 * <p>Delivery is currently a log statement, since no email or push infrastructure exists yet; this
 * is the seam where a real delivery channel will later plug in.
 *
 * <p>Two observers are equal when they represent the same user, so a user is only ever attached to
 * an observable once, and can be detached without using the original instance.
 */
public class UserEventObserver implements IObserver {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(UserEventObserver.class);

  private final UUID userId;

  public UserEventObserver(UUID userId) {
    this.userId = userId;
  }

  @Override
  public void update(IObservable source, NotificationType notification) {
    LOGGER.info("Notifying user {} of {} in event {}", userId, notification, source.getEventId());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof UserEventObserver)) {
      return false;
    }
    UserEventObserver castedOther = (UserEventObserver) other;
    return castedOther.userId.equals(userId);
  }

  @Override
  public int hashCode() {
    return userId.hashCode();
  }

}
