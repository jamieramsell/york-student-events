package york.studentevents.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

/**
 * Generic repository interface providing standard CRUD operations.
 * All domain-specific repository interfaces should extend this interface.
 *
 * @param <T> the type of entity managed by this repository
 */
public interface IRepository<T extends IEntity> {
  /**
   * Saves an entity to the repository. If an entity with the same ID already
   * exists, it is overwritten.
   *
   * @param entity the entity to save; must not be {@code null}
   */
  void save(T entity);

  /**
   * Removes the entity with the given ID from the repository.
   *
   * @param id the ID of the entity to remove
   * @throws java.util.NoSuchElementException if no entity with the given ID exists
   */
  void delete(UUID id);

  /**
   * Looks up an entity by its ID.
   *
   * @param id the ID of the entity to retrieve
   * @return an {@link Optional} containing the entity if one exists, or {@link Optional#empty()}
   *     if no entity with the given ID is found
   */
  Optional<T> findByID(UUID id);

  /**
   * Retrieves all entities from the repository in pages.
   *
   * @param pageNumber the page number to retrieve; must be greater than or equal to 0.
   * @param pageSize the number of entities to retrieve per page; must be greater than 0,
   *                 and less than or equal to 100.
   * @return a {@link Page} of entities; never {@code null}, but may be empty.
   * @throws IllegalArgumentException if {@code pageNumber} or {@code pageSize} are invalid.
   */
  Page<T> findAll(int pageNumber, int pageSize);
}
