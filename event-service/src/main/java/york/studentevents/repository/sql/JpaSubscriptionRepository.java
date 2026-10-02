package york.studentevents.repository.sql;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import york.studentevents.subscriptions.Subscription;

/**
 * JPA backed repository for storing and retrieving {@link Subscription} entities.
 *
 * <p>Extends {@link JpaRepository} with {@link Subscription} as the entity type, and {@link UUID}
 *     as the key type, providing standard CRUD operations scoped to Subscriptions.
 *
 * <p>Should be used in conjunction with the {@link SubscriptionRepositoryAdapter} to map methods
 *     onto the {@link york.studentevents.subscriptions.ISubscriptionRepository} interface.
 *
 * @see york.studentevents.repository.IRepository
 * @see york.studentevents.subscriptions.ISubscriptionRepository
 * @see york.studentevents.subscriptions.ISubscription
 */
@Repository
public interface JpaSubscriptionRepository extends JpaRepository<Subscription, UUID> {

  List<Subscription> findByUserIdAndEventId(UUID userId, UUID eventId);

  List<Subscription> findByEventId(UUID eventId);

  List<Subscription> findByUserId(UUID userId);

}
