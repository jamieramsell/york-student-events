package york.studentevents.repository.inmemory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    List<ISubscription> targetSubscriptions = hashMap.values().stream()
        .filter(sub -> sub.getUserId().equals(userId))
        .filter(sub -> sub.getEventId().equals(eventId))
        .toList();

    // Validate that a maximum of one subscription exists
    if (targetSubscriptions.size() > 1) {
      throw new IllegalStateException("The given user has multiple subscription records targeting"
      + " the specified event.");
    }

    return targetSubscriptions.stream().findFirst();
  }

  @Override
  public Page<ISubscription> findAllByUserId(UUID userId, int pageNumber, int pageSize) {
    if (userId == null) {
      throw new IllegalArgumentException("userId cannot be null");
    }
    if (pageNumber < 0) {
      throw new IllegalArgumentException("pageNumber must not be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be greater than zero");
    }

    List<ISubscription> returnList = new ArrayList<>(hashMap.values());
    returnList.sort(Comparator.comparing(ISubscription::getId));
    returnList = returnList.stream().filter(sub -> sub.getUserId().equals(userId)).toList();

    long startIndexLong = (long) pageNumber * pageSize;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    if (startIndexLong >= returnList.size()) {
      return new PageImpl<>(List.of(), pageable, returnList.size());
    }

    int startIndex = Math.toIntExact(startIndexLong);
    int endIndex = Math.min(startIndex + pageSize, returnList.size());

    List<ISubscription> pageContent = new ArrayList<>(returnList.subList(startIndex, endIndex));
    return new PageImpl<>(pageContent, pageable, returnList.size());
  }

  @Override
  public Page<ISubscription> findAllByEventId(UUID eventId, int pageNumber, int pageSize) {
    if (eventId == null) {
      throw new IllegalArgumentException("eventId cannot be null");
    }
    if (pageNumber < 0) {
      throw new IllegalArgumentException("pageNumber must not be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be greater than zero");
    }

    List<ISubscription> returnList = new ArrayList<>(hashMap.values());
    returnList.sort(Comparator.comparing(ISubscription::getId));
    returnList = returnList.stream().filter(sub -> sub.getEventId()Id().equals(eventId)).toList();

    long startIndexLong = (long) pageNumber * pageSize;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    if (startIndexLong >= returnList.size()) {
      return new PageImpl<>(List.of(), pageable, returnList.size());
    }

    int startIndex = Math.toIntExact(startIndexLong);
    int endIndex = Math.min(startIndex + pageSize, returnList.size());

    List<ISubscription> pageContent = new ArrayList<>(returnList.subList(startIndex, endIndex));
    return new PageImpl<>(pageContent, pageable, returnList.size());
  }

}
