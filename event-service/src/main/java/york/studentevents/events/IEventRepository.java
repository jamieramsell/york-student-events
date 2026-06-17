package york.studentevents.events;

import york.studentevents.repository.IRepository;

/**
 * Repository for storing and retrieving {@link Event} entities.
 *
 * <p>Extends {@link york.studentevents.repository.IRepository} with {@link Event} as the
 * managed type, providing standard CRUD operations ({@code save}, {@code findAll})
 * scoped to events.
 *
 * @see york.studentevents.repository.IRepository
 * @see Event
 */
public interface IEventRepository extends IRepository<Event> {}
