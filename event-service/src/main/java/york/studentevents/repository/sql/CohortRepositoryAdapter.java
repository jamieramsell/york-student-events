package york.studentevents.repository.sql;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.cohorts.Cohort;
import york.studentevents.cohorts.ICohort;
import york.studentevents.cohorts.ICohortRepository;

/**
 * Database-backed implementation of {@link ICohortRepository}, adapting Spring Data JPA to the
 * domain repository contract.
 *
 * <p>Persistence is delegated to a {@link JpaCohortRepository}, whose implementation is generated
 *     by Spring Data at runtime. This adapter translates between the two layers: the concrete
 *     {@link Cohort} entity is widened to the {@link ICohort} interface on the way out, a missing
 *     row is mapped to {@link Optional#empty()}, and a delete against a missing row is reported as
 *     the {@link java.util.NoSuchElementException} the
 *     {@link york.studentevents.repository.IRepository} contract requires, rather than the
 *     framework exception that Spring Data would otherwise raise.
 *
 * <p>This is the intended production implementation of {@link ICohortRepository}, superseding the
 *     hash-map backed {@link york.studentevents.repository.inmemory.InMemoryCohortRepository}.
 *
 * @see ICohortRepository
 * @see JpaCohortRepository
 * @see ICohort
 */
public class CohortRepositoryAdapter implements ICohortRepository {

  private final JpaCohortRepository jpa;

  /**
   * Constructs an adapter that delegates persistence to the given Spring Data repository.
   *
   * @param jpa the Spring Data repository to delegate to; must not be {@code null}
   */
  public CohortRepositoryAdapter(JpaCohortRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void save(ICohort entity) {
    Cohort concreteEntity = (Cohort) entity;
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
  public Optional<ICohort> findByID(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    Optional<Cohort> concreteEntity = jpa.findById(id);

    // Cast the concrete entity to its interface
    Optional<ICohort> interfaceEntity;
    try {
      interfaceEntity = Optional.of((ICohort) concreteEntity.get());
    } catch (NoSuchElementException e) {
      interfaceEntity = Optional.empty();
    }
    return interfaceEntity;
  }

  @Override
  public List<ICohort> findAll() {
    return jpa.findAll()
        .stream()
        .map((e) -> (ICohort) e)
        .toList();
  }
}
