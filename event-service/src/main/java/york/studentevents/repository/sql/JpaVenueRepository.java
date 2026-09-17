package york.studentevents.repository.sql;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import york.studentevents.venues.Venue;

/**
 * JPA backed repository for storing and retrieving {@link Venue} entities.
 *
 * <p>Extends {@link JpaRepository} with {@link Venue} as the entity type, and {@link UUID} as the
 *     key type, providing standard CRUD operations scoped to Venues.
 *
 * <p>Should be used in conjunction with the {@link VenueRepositoryAdapter} to map methods onto the
 *     {@link york.studentevents.venues.IVenueRepository} interface.
 *
 * @see york.studentevents.repository.IRepository
 * @see york.studentevents.venues.IVenueRepository
 * @see york.studentevents.venues.Venue
 */
@Repository
public interface JpaVenueRepository extends JpaRepository<Venue, UUID> {}
