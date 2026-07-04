package york.studentevents.subscriptions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.repository.IRepository;

/**
 * Repository for storing and retrieving {@link ISubscription} entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link ISubscription} as the
 * managed type, providing both standard and specialised CRUD operations scoped to subscriptions.
 *
 * @see york.studentevents.repository.IRepository
 * @see ISubscription
 */
public interface ISubscriptionRepository extends IRepository<ISubscription> {

  /**
   * Retrieves all subscriptions of a given User.
   *
   * @param userId the user's ID
   * @return a {@link List} of the user's subscriptions; never {@code null}, but may be empty
   */
  List<ISubscription> findAllByUserId(UUID userId);

  /**
   * Retrieves all subscriptions to a given Event.
   *
   * @param eventId the events's ID
   * @return a {@link List} of subscriptions to the Event; never {@code null}, but may be empty
   */
  List<ISubscription> findAllByEventId(UUID eventId);

  /**
   * Looks up a Subscription by its user and event IDs.
   *
   * @param userId the user's ID
   * @param eventId the event's ID
   * @return an {@link Optional} containing the entity if one exists, or {@link Optional#empty()}
   *     if no entity with the given ID is found
   */
  Optional<ISubscription> findByID(UUID userId, UUID eventId);

}
