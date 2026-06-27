package york.studentevents.subscriptions;

import york.studentevents.events.IEvent;

/**
 * Defines the contract for any class that wants to receive event notifications.
 *
 * <p>Forms part of the Observer pattern implementation.
 */
public interface IObserver {

  /**
   * Method, called from an IObservable, whenever information about the observable event is updated.
   * 
   * @param source The object being observed.
   * @param event The updated state of the event in question. 
   */
  void update(IObservable source, IEvent event);

}
