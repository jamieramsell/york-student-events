package york.studentevents.repository.inmemory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import york.studentevents.repository.IEntity;
import york.studentevents.repository.IRepository;

/**
 * Hash-map backed repository for storing and retrieving entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link IEntity} as the
 * managed type, providing standard CRUD operations which are accessible by {@code UUID}.
 *
 * <p>Abstract class used as a generic common ancestor for all memory-based repositories.
 *
 * <p>Used for integration testing before implementing database-backed repositories.
 *
 * @see york.studentevents.repository.IRepository
 * @see IEntity
 * @see UUID
 */
abstract class AbstractInMemoryRepository<T extends IEntity> implements IRepository<T> {
  
  protected Map<UUID, T> hashMap;

  /** Constructs a new, empty, memory-implemented repository. */
  public AbstractInMemoryRepository() {
    hashMap = new HashMap<>();
  }

  @Override
  public void save(T entity) {
    hashMap.put(entity.getId(), entity);
  }

  @Override
  public void delete(UUID id) {
    if (hashMap.remove(id) == null) {
      throw new NoSuchElementException("No entity with ID " + id + " exists.");
    }
  }

  @Override
  public Optional<T> findByID(UUID id) {
    Optional<T> entity = Optional.ofNullable(hashMap.get(id));
    return entity;
  }

  @Override
  public Page<T> findAll(int pageNumber, int pageSize) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("pageNumber must not be negative");
    }
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be greater than zero");
    }

    List<T> returnList = new ArrayList<>(hashMap.values());
    returnList.sort(Comparator.comparing(T::getId));

    long startIndexLong = (long) pageNumber * pageSize;
    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    if (startIndexLong >= returnList.size()) {
      return new PageImpl<>(List.of(), pageable, returnList.size());
    }

    int startIndex = Math.toIntExact(startIndexLong);
    int endIndex = Math.min(startIndex + pageSize, returnList.size());

    List<T> pageContent = new ArrayList<>(returnList.subList(startIndex, endIndex));
    return new PageImpl<>(pageContent, pageable, returnList.size());
  }

}
