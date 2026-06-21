package york.studentevents.subscriptions;

import york.studentevents.events.IEvent;

/**
 * Interface for observers of {@link IEvent}s.
 */
public interface IObserver {
  /**
   * Called when an event is updated.
   *
   * @param event the updated event
   */
  void update(IEvent event);
}