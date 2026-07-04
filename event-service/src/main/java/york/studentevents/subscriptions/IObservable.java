package york.studentevents.subscriptions;

import york.studentevents.events.IEvent;

/**
 * Defines the contract for any class that can be observed.
 *
 * <p>Forms part of the Observer pattern implementation.
 */
public interface IObservable {

  /**
   * Attaches, or subscribes, an observer to this observable object, meaning it will receive
   * notifications whenever the target event is updated.
   *
   * @param observer The observer to attach to this observable object.
   * @return true if the observer was attached; false if the observer was already attached to begin
   *     with.
   */
  boolean attach(IObserver observer);
  
  /**
   * Dettaches, or unsubscribes, an observer from this observable object, meaning it will no longer
   * receive notifications if the target event gets updated.
   *
   * @param observer The observer to detatch from this observable object.
   * @return true if the observer was detached; false if the observer wasn't attached to begin with.
   */
  boolean detach(IObserver observer);
  
  /**
   * Notifies every attached observer of a change to the given event.
   *
   * <p>Called by the observable whenever an {@code Event} is updated, fanning the change out to
   * each {@link IObserver} currently attached.
   *
   * <p>The order in which observers are notified is not guaranteed.
   *
   * <p>If no observers are attached, this method is a no-op.
   *
   * <p>Forms part of the Observer pattern implementation.
   *
   * @param reason the type of change to the event; the reason why the notification is being sent.
   *
   * @see IObserver
   * @see #attach(IObserver)
   * @see #detach(IObserver)
   */
  void notifyObservers(NotificationType reason);
  
}
