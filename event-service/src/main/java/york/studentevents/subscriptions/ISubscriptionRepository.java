package york.studentevents.subscriptions;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import york.studentevents.events.IEvent;
import york.studentevents.repository.IRepository;
import york.studentevents.users.IUser;

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
   * Retrieves all subscriptions of a given {@link IUser} in pages.
   *
   * @param userId the user's ID.
   * @param pageNumber the page number to retrieve; must be greater than or equal to 0.
   * @param pageSize the number of entities to retrieve per page; must be greater than 0, and less
   *     than or equal to 100.
   * @return a {@link Page} of {@link IUser}; never {@code null}, but may be empty.
   * @throws IllegalArgumentException if {@code pageNumber}, {@code pageSize}, or {@code userId}
   *        are invalid or null.
   */
  Page<ISubscription> findAllByUserId(UUID userId, int pageNumber, int pageSize);

  /**
   * Retrieves all subscriptions to a given {@link IEvent} in pages.
   *
   * @param eventId the events' ID.
   * @param pageNumber the page number to retrieve; must be greater than or equal to 0.
   * @param pageSize the number of entities to retrieve per page; must be greater than 0,
   *                 and less than or equal to 100.
   * @return a {@link Page} of {@link IEvent}; never {@code null}, but may be empty.
   * @throws IllegalArgumentException if {@code pageNumber}, {@code pageSize}, or {@code eventId}
   *        are invalid or null.
   */
  Page<ISubscription> findAllByEventId(UUID eventId, int pageNumber, int pageSize);

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
