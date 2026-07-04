package york.studentevents.subscriptions;

/** Represents the type of notification broadcasted to subscribed users. */
public enum NotificationType {
  
  /** The capacity warning threshold has been reached, and the event is nearing max capacity. */
  CAPACITY_WARNING,

  /** The event has now reached maximum capacity. */
  CAPACITY_REACHED,

  /** The time of the event has changed, however its date has not. */
  TIME_CHANGE,

  /** The date (and possibly the timing) of the event has changed. */
  DATE_CHANGE,

  /** The event is now being managed by another host. */
  HOST_CHANGE,

  /** The location of the event has changed. */
  VENUE_CHANGE,

  /** The event description has been changed. */
  DESCRIPTION_CHANGE

}
