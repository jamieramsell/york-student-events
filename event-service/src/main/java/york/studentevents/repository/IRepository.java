package york.studentevents.repository;

import java.util.Optional;
import java.util.List;

public interface IRepository<T>{
    void save (T entity);
    void delete (Long id);
    Optional <T> findByID(Long id);
    List <T> findAll();

}
