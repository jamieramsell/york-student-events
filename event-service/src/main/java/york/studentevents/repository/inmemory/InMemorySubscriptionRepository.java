package york.studentevents.repository.inmemory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import york.studentevents.subscriptions.ISubscription;
import york.studentevents.subscriptions.ISubscriptionRepository;

/**
 * Hash-map backed repository for storing and retrieving {@link ISubscription} entities.
 *
 * <p>Extends {@link york.studentevents.repository.inmemory.AbstractInMemoryRepository} with
 * {@link ISubscription} as the managed type, providing both standard and specialised CRUD
 * operations scoped to subscriptions.
 *
 * <p>Used for integration testing before implementing database-backed repositories.
 *
 * @see york.studentevents.repository.IRepository
 * @see AbstractInMemoryRepository
 * @see ISubscription
 */
public class InMemorySubscriptionRepository extends AbstractInMemoryRepository<ISubscription>
    implements ISubscriptionRepository {
  
  @Override
  public Optional<ISubscription> findByID(UUID userId, UUID eventId) {
    Stream<ISubscription> targetSubscriptions = findAll()
        .stream()
        .filter(sub -> sub.getUserId().equals(userId))
        .filter(sub -> sub.getEventId().equals(eventId));

    // Validate that a maximum of one subscription exists
    if (targetSubscriptions.count() > 1) {
      throw new IllegalStateException("The given user has multiple subscription records"
          + " targetting the specified event.");
    }

    return targetSubscriptions.findFirst();
  }

  @Override
  public List<ISubscription> findAllByUserId(UUID userId) {
    List<ISubscription> subscriptions = findAll()
        .stream()
        .filter(sub -> sub.getUserId().equals(userId))
        .toList();

    return subscriptions;
  }

  @Override
  public List<ISubscription> findAllByEventId(UUID eventId) {
    List<ISubscription> subscriptions = findAll()
        .stream()
        .filter(sub -> sub.getEventId().equals(eventId))
        .toList();

    return subscriptions;
  }

}
