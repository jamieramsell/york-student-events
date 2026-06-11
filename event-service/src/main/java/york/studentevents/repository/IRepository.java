package york.studentevents.repository;

import java.util.Optional;
import java.util.List;

public interface IRepository<T>{
    /**
     * The selected entity is saved to repo.
     * @param entity saves a selected entity
     */
    void save (T entity);
    
    /**
     * Selects an id, removes that entity from the repository - nothing is returned
     * @param id selected is removed
     */
    void delete (Long id);
    
    /**
     * Finds an entity by its unique identifier
     * @param id the unique identifier of the entity
     * @return an Optional containing the entity if found, empty if not
     */
    Optional <T> findById(Long id);

    /**
     * Retrieves all entities from the repository
     * @return List of all entities, or an empty List if none exist
     */
    List <T> findAll();

}
