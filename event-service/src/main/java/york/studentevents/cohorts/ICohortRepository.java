package york.studentevents.cohorts;

import york.studentevents.repository.IRepository;

/**
 * Repository for storing and retrieving {@link ICohort} entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link ICohort} as the
 * managed type, providing standard CRUD operations scoped to cohorts.
 *
 * @see york.studentevents.repository.IRepository
 * @see ICohort
 */
public interface ICohortRepository extends IRepository<ICohort> {}
