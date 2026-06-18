package york.studentevents.repository;

import java.util.List;

/**
 * Generic repository interface providing standard CRUD operations.
 * All domain-specific repository interfaces should extend this interface.
 *
 * @param <T> the type of entity managed by this repository
 */
public interface IRepository<T> {

  /**
   * Saves an entity to the repository. If an entity with the same ID already
   * exists, it is overwritten.
   *
   * @param entity the entity to save; must not be {@code null}
   */
  void save(T entity);

  /**
   * Retrieves all entities currently held in the repository.
   *
   * @return a {@link List} of all entities; never {@code null}, but may be empty
   */
  List<T> findAll();

}
