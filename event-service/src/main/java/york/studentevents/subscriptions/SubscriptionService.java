package york.studentevents.subscriptions;

import java.util.Optional;
import java.util.UUID;
import york.studentevents.subscriptions.ISubscription.SubscriptionSource;

/**
 * Application service coordinating event subscriptions and the notifications sent to subscribers.
 *
 * <p>Sits over the {@link ISubscriptionRepository} (the durable record of who subscribes to which
 * event) and the Observer pattern, which delivers notifications. It records and reconciles
 * subscriptions, reports whether a user is subscribed, and publishes a {@link NotificationType} to
 * an event's subscribers by populating an {@link EventNotificationService} from the stored records.
 *
 * <p>A user holds at most one subscription per event. Explicit subscriptions are sticky:
 * registration never overwrites one and deregistration never removes one; only an explicit
 * unsubscribe does.
 *
 * <p>Note that no validation is performed against the user or event IDs passed in. That logic is
 * delegated to the {@code StudentEventService} which owns the instance of
 * {@code SubscriptionService}.
 *
 * @see ISubscriptionRepository
 * @see EventNotificationService
 * @see NotificationType
 */
public class SubscriptionService {

  private final ISubscriptionRepository subscriptionRepository;

  /**
   * Constructs an instance of SubscriptionService; injects it with a given subscription repository.
   *
   * @param subscriptionRepository the subscription repository to inject.
   *
   * @see ISubscriptionRepository
   */
  public SubscriptionService(ISubscriptionRepository subscriptionRepository) {
    this.subscriptionRepository = subscriptionRepository;
  }

  /**
   * Subscribes a User to a given Event, reconciling the request with any subscription the user
   * already holds for the event. A user holds at most one subscription per event.
   *
   * <p>A {@link SubscriptionSource#REGISTRATION} request creates a registration-based subscription
   * only when the user has none. It never disturbs an existing subscription, so an explicit
   * subscription stays sticky and takes precedence.
   *
   * <p>A {@link SubscriptionSource#EXPLICIT} request ensures an explicit, sticky subscription: a
   * registration-based subscription is upgraded to explicit so that it survives a later
   * deregistration, and subscribing explicitly when already explicitly subscribed fails.
   *
   * <p>Note that, due to the nature of {@code SubscriptionService}, no validation is performed
   * against the IDs provided. This logic should be delegated to the {@code StudentEventService}
   * which owns the instance of {@code SubscriptionService}.
   *
   * @param userId The user's ID
   * @param eventId The event's ID
   * @param reason The reason why the subscription is being created.
   * @throws IllegalArgumentException if {@code reason} is {@link SubscriptionSource#EXPLICIT} and
   *     the user is already explicitly subscribed to the event.
   *
   * @see SubscriptionSource
   */
  public void subscribe(UUID userId, UUID eventId, SubscriptionSource reason) {
    // Check whether the user is already subscribed to the event
    Optional<ISubscription> existingSubscription = subscriptionRepository.findByID(userId, eventId);

    // Registration never disturbs an existing subscription, but explicit subscriptions are sticky
    switch (reason) {
      
      case SubscriptionSource.REGISTRATION -> {
        if (existingSubscription.isEmpty()) {
          subscriptionRepository.save(new Subscription(userId, eventId, reason));
        }
        return;
      }
      
      case SubscriptionSource.EXPLICIT -> {
        if (existingSubscription.isPresent()) {
          // If the existing subscription is already EXPLICIT, throw
          if (existingSubscription.get().getSource() == SubscriptionSource.EXPLICIT) {
            throw new IllegalArgumentException(String.format(
                "User %s is already subscribed to event %s", userId, eventId
            ));
          }
        }

        // Save a new subsciption to the repository, overriding the existing one if such it exists.
        if (existingSubscription.isPresent()) {
          UUID subscriptionId = existingSubscription.get().getId();
          subscriptionRepository.save(new Subscription(subscriptionId, userId, eventId, reason));
        } else {
          subscriptionRepository.save(new Subscription(userId, eventId, reason));
        }
      }

      default -> throw new IllegalStateException("Unhandled subscription source: " + reason);
    }
  }

  /**
   * Removes a User's subscription to a given Event, with the effect depending on why the
   * unsubscription is requested. A user holds at most one subscription per event.
   *
   * <p>A {@link SubscriptionSource#REGISTRATION} request (a deregistration) removes only a
   * registration-based subscription. An explicit subscription is left untouched, and if no
   * matching subscription exists the state is simply left unaffected: this is not an error.
   *
   * <p>A {@link SubscriptionSource#EXPLICIT} request (an explicit unsubscription) removes the
   * user's subscription regardless of how it was created.
   *
   * <p>Note that, due to the nature of {@code SubscriptionService}, no validation is performed
   * against the IDs provided. This logic should be delegated to the {@code StudentEventService}
   * which owns the instance of {@code SubscriptionService}.
   *
   * @param userId The user's ID
   * @param eventId The event's ID
   * @param reason The reason why the subscription is being removed.
   * @throws IllegalArgumentException if {@code reason} is {@link SubscriptionSource#EXPLICIT} and
   *     the user is not subscribed to the event.
   *
   * @see SubscriptionSource
   */
  public void unsubscribe(UUID userId, UUID eventId, SubscriptionSource reason) {
    // Check whether the user is already subscribed to the event
    Optional<ISubscription> existingSubscription = subscriptionRepository.findByID(userId, eventId);

    switch (reason) {
      case SubscriptionSource.REGISTRATION -> {
        // Deregistration removes only a registration-based sub; explicit subs are untouched, and a
        // missing subscription is fine: state is unaffected.
        existingSubscription
            .filter(sub -> sub.getSource() == SubscriptionSource.REGISTRATION)
            .ifPresent(sub -> subscriptionRepository.delete(sub.getId()));
        return;
      }

      case SubscriptionSource.EXPLICIT -> {
        // EXPLICIT: removes the subscription regardless of how it was created.
        ISubscription subscription = existingSubscription.orElseThrow(() ->
            new IllegalArgumentException(
                String.format("User %s is not subscribed to event %s", userId, eventId)
            ));
        subscriptionRepository.delete(subscription.getId());
      }

      default -> throw new IllegalStateException("Unhandled subscription source: " + reason);
    }
  }

  /**
   * Publish a notification to the subscribers of an event.
   *
   * <p>Creates an instance of {@link EventNotificationService}, which is used to notify a
   * {@link UserEventObserver} of each User subscribed to the Event.
   *
   * <p>Note that, due to the nature of {@code SubscriptionService}, no validation is performed to
   * check whether an Event with the given ID exists; this logic should be delegated to the
   * {@code StudentEventService} which owns the instance of {@code SubscriptionService}.
   *
   * @param eventId The event about which to publish a notification.
   * @param reason The reason for the notification
   *
   * @see NotificationType
   * @see EventNotificationService
   */
  public void publish(UUID eventId, NotificationType reason) {
    EventNotificationService channel = new EventNotificationService(eventId);

    // Attach all subscribers as UserEventObservers
    subscriptionRepository.findAllByEventId(eventId)
        .stream()
        .forEach(subscription -> channel.attach(new UserEventObserver(subscription.getUserId())));

    channel.notifyObservers(reason);
  }

  /**
   * Check whether a User is subscribed to a given Event.
   *
   * <p>Note that, due to the nature of {@code SubscriptionService}, no validation is performed
   * against the IDs provided. This logic should be delegated to the {@code StudentEventService}
   * which owns the instance of {@code SubscriptionService}.
   *
   * @param userId The user's ID
   * @param eventId The event's ID
   * @return {@code true} if the user is subscribed to the given event; otherwise {@code false}.
   */
  public boolean isSubscribed(UUID userId, UUID eventId) {
    return subscriptionRepository.findByID(userId, eventId).isPresent();
  }

}
