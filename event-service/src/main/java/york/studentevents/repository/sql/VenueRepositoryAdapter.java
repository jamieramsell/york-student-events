package york.studentevents.repository.sql;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.venues.IVenue;
import york.studentevents.venues.IVenueRepository;
import york.studentevents.venues.Venue;

/**
 * Database-backed implementation of {@link IVenueRepository}, adapting Spring Data JPA to the
 * domain repository contract.
 *
 * <p>Persistence is delegated to a {@link JpaVenueRepository}, whose implementation is generated
 *     by Spring Data at runtime. This adapter translates between the two layers: the concrete
 *     {@link Venue} entity is widened to the {@link IVenue} interface on the way out, a missing row
 *     is mapped to {@link Optional#empty()}, and a delete against a missing row is reported as the
 *     {@link java.util.NoSuchElementException} that the
 *     {@link york.studentevents.repository.IRepository} contract requires, rather than the
 *     framework exception Spring Data would otherwise raise.
 *
 * <p>This is the intended production implementation of {@link IVenueRepository}, superseding the
 *     hash-map backed {@link york.studentevents.repository.inmemory.InMemoryVenueRepository}.
 *
 * @see IVenueRepository
 * @see JpaVenueRepository
 * @see IVenue
 */
public class VenueRepositoryAdapter implements IVenueRepository {

  private final JpaVenueRepository jpa;

  /**
   * Constructs an adapter that delegates persistence to the given Spring Data repository.
   *
   * @param jpa the Spring Data repository to delegate to; must not be {@code null}
   */
  public VenueRepositoryAdapter(JpaVenueRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void save(IVenue entity) {
    Venue concreteEntity = (Venue) entity;
    if (concreteEntity == null) {
      throw new IllegalArgumentException("entity cannot be null");
    }
    jpa.saveAndFlush(concreteEntity);
  }

  @Override
  public void delete(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if (!jpa.existsById(id)) {
      throw new NoSuchElementException("No entity with the given ID exists within the database.");
    }
    jpa.deleteById(id);
  }

  @Override
  public Optional<IVenue> findByID(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    Optional<Venue> concreteVenue = jpa.findById(id);

    // Cast the concrete venue to its interface
    Optional<IVenue> interfaceVenue;
    try {
      interfaceVenue = Optional.of((IVenue) concreteVenue.get());
    } catch (NoSuchElementException e) {
      interfaceVenue = Optional.empty();
    }
    return interfaceVenue;
  }

  @Override
  public List<IVenue> findAll() {
    return jpa.findAll()
        .stream()
        .map((e) -> (IVenue) e)
        .toList();
  }
}
