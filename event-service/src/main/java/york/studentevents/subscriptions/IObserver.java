package york.studentevents.subscriptions;

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
   * @param reason The reason for the notification. 
   */
  void update(IObservable source, NotificationType reason);

}
