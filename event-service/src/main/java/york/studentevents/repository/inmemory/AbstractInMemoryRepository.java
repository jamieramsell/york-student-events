package york.studentevents.repository.inmemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
 *
 * @see IEntity
 *
 * @see UUID
 */
abstract class AbstractInMemoryRepository<T extends IEntity> implements IRepository<T> {
  
  private Map<UUID, T> hashMap;

  /** Constructs a new, empty, memory implemented repository. */
  public AbstractInMemoryRepository() {
    hashMap = new HashMap<>();
  }

  @Override
  public void save(T entity) {
    hashMap.put(entity.getId(), entity);
  }

  @Override
  public void delete(UUID id) {
    hashMap.remove(id);
  }

  @Override
  public Optional<T> findByID(UUID id) {
    Optional<T> entity = Optional.ofNullable(hashMap.get(id));
    return entity;
  }

  @Override
  public List<T> findAll() {
    List<T> returnList = new ArrayList<>();
    returnList.addAll(hashMap.values());
    return returnList;
  }

}
