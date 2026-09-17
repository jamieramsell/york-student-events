package york.studentevents.repository.sql;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import york.studentevents.cohorts.Cohort;

/**
 * JPA backed repository for storing and retrieving {@link Cohort} entities.
 *
 * <p>Extends {@link JpaRepository} with {@link Cohort} as the entity type, and {@link UUID} as the
 *     key type, providing standard CRUD operations scoped to Users.
 *
 * <p>Should be used in conjunction with the {@link CohortRepositoryAdapter} to map methods onto the
 *     {@link york.studentevents.users.ICohortRepository} interface.
 *
 * @see york.studentevents.repository.IRepository
 * @see york.studentevents.cohorts.ICohortRepository
 * @see york.studentevents.cohorts.ICohort
 */
@Repository
public interface JpaCohortRepository extends JpaRepository<Cohort, UUID> {}
