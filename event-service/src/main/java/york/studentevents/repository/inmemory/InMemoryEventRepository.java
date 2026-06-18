package york.studentevents.repository.inmemory;

import york.studentevents.events.IEvent;

/**
 * Hash-map backed repository for storing and retrieving {@link IEvent} entities.
 *
 * <p>Extends {@link york.studentevents.repository.AbstractInMemoryRepository} with {@link IEvent}
 * as the managed type, providing standard CRUD operations scoped to events.
 *
 * <p>Used for integration testing before implementing database-backed repositories.
 * 
 * @see york.studentevents.repository.IRepository
 * @see AbstractInMemoryRepository
 * @see IEvent
 */
public class InMemoryEventRepository extends AbstractInMemoryRepository<IEvent> {}