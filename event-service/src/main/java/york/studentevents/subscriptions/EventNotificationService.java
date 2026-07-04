package york.studentevents.subscriptions;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An {@link IObservable} that broadcasts notifications about a single event to its attached
 * observers.
 *
 * <p>Forms the subject in the platform's Observer pattern: an {@link IObserver} calls
 * {@link #attach} to start receiving notifications and {@link #detach} to stop. Each instance is
 * bound to one event (by ID) and fans a {@link NotificationType} out to every currently attached
 * observer; the order of notification is not guaranteed.
 *
 * <p>One instance is dedicated to a single event. {@link SubscriptionService} builds one from an
 * event's subscribers when it publishes a notification.
 *
 * @see IObservable
 * @see IObserver
 * @see SubscriptionService
 */
public class EventNotificationService implements IObservable {

  private final Set<IObserver> observers;
  private final UUID eventId;

  /**
   * Creates an observable bound to the given event, with no observers attached.
   *
   * @param eventId the ID of the event this observable broadcasts notifications for
   */
  public EventNotificationService(UUID eventId) {
    observers = new HashSet<>();
    this.eventId = eventId;
  }

  @Override
  public boolean attach(IObserver observer) {
    return observers.add(observer);
  }

  @Override
  public boolean detach(IObserver observer) {
    return observers.remove(observer);
  }

  @Override
  public void notifyObservers(NotificationType reason) {
    for (IObserver observer : observers) {
      observer.update(this, reason);
    }
  }

  @Override
  public UUID getEventId() {
    return eventId;
  }

}
