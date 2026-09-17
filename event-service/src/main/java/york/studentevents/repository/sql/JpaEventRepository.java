package york.studentevents.repository.sql;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import york.studentevents.events.Event;

/**
 * JPA backed repository for storing and retrieving {@link york.studentevents.events.IEvent}
 * entities.
 *
 * <p>Extends {@link JpaRepository} with {@link Event} as the entity type, and {@link UUID} as the
 *     key type, providing standard CRUD operations scoped to Events.
 *
 * <p>Should be used in conjunction with the {@link EventRepositoryAdapter} to map methods onto the
 *     {@link york.studentevents.events.IEventRepository} interface.
 *
 * @see york.studentevents.repository.IRepository
 * @see york.studentevents.events.IEventRepository
 * @see york.studentevents.events.IEvent
 */
@Repository
public interface JpaEventRepository extends JpaRepository<Event, UUID> {}
