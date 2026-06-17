package york.studentevents.events;

import york.studentevents.repository.IRepository;

/**
 * Repository for storing and retrieving {@link IEvent} entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link IEvent} as the
 * managed type, providing standard CRUD operations scoped to events.
 *
 * @see york.studentevents.repository.IRepository
 * @see IEvent
 */
public interface IEventRepository extends IRepository<IEvent> {}
