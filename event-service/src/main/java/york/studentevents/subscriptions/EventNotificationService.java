package york.studentevents.subscriptions;

import java.util.HashSet;
import java.util.Set;
import york.studentevents.events.IEvent;

/** todo. */
public class EventNotificationService implements IObservable {

  private final Set<IObserver> observers;

  /** todo. */
  public EventNotificationService() {
    observers = new HashSet<>();
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
  public void notifyObservers(IEvent event) {
    for (IObserver observer : observers) {
      observer.update(this, event);
    }
  }

}
