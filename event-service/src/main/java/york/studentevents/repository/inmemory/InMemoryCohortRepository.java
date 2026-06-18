package york.studentevents.repository.inmemory;

import york.studentevents.cohorts.ICohort;

/**
 * Hash-map backed repository for storing and retrieving {@link ICohort} entities.
 *
 * <p>Extends {@link york.studentevents.repository.inmemory.AbstractInMemoryRepository} with {@link ICohort}
 * as the managed type, providing standard CRUD operations scoped to cohorts.
 *
 * <p>Used for integration testing before implementing database-backed repositories.
 * 
 * @see york.studentevents.repository.IRepository
 * @see AbstractInMemoryRepository
 * @see ICohort
 */
public class InMemoryCohortRepository extends AbstractInMemoryRepository<ICohort> {}
