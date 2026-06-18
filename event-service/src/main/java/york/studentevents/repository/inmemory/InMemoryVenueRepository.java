package york.studentevents.repository.inmemory;

import york.studentevents.venues.IVenue;

/**
 * Hash-map backed repository for storing and retrieving {@link IVenue} entities.
 *
 * <p>Extends {@link york.studentevents.repository.AbstractInMemoryRepository} with {@link IVenue}
 * as the managed type, providing standard CRUD operations scoped to venues.
 *
 * <p>Used for integration testing before implementing database-backed repositories.
 * 
 * @see york.studentevents.repository.IRepository
 * @see AbstractInMemoryRepository
 * @see IVenue
 */
public class InMemoryVenueRepository extends AbstractInMemoryRepository<IVenue> {}
