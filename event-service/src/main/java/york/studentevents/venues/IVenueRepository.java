package york.studentevents.venues;

import york.studentevents.repository.IRepository;

/**
 * Repository for storing and retrieving {@link IVenue} entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link IVenue} as the
 * managed type, providing standard CRUD operations scoped to vsnuew.
 *
 * @see york.studentevents.repository.IRepository
 * @see IVenue
 */
public interface IVenueRepository extends IRepository<IVenue> {}
