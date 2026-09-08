package york.studentevents.repository.sql;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.events.Event;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;

/**
 * Database-backed implementation of {@link IEventRepository}, adapting Spring Data JPA to the
 * domain repository contract.
 *
 * <p>Persistence is delegated to a {@link JpaEventRepository}, whose implementation is generated
 *     by Spring Data at runtime. This adapter translates between the two layers: the concrete
 *     {@link Event} entity is widened to the {@link IEvent} interface on the way out, a missing row
 *     is mapped to {@link Optional#empty()}, and a delete against a missing row is reported as the
 *     {@link java.util.NoSuchElementException} the
 *     {@link york.studentevents.repository.IRepository} contract requires, rather than the
 *     framework exception Spring Data would otherwise raise.
 *
 * <p>This is the intended production implementation of {@link IEventRepository}, superseding the
 *     hash-map backed {@link york.studentevents.repository.inmemory.InMemoryEventRepository}.
 *
 * @see IEventRepository
 * @see JpaEventRepository
 * @see IEvent
 */
public class EventRepositoryAdapter implements IEventRepository {

  private final JpaEventRepository jpa;

  /**
   * Constructs an adapter that delegates persistence to the given Spring Data repository.
   *
   * @param jpa the Spring Data repository to delegate to; must not be {@code null}
   */
  public EventRepositoryAdapter(JpaEventRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public void save(IEvent entity) {
    Event concreteEntity = (Event) entity;
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
  public Optional<IEvent> findByID(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    Optional<Event> concreteEvent = jpa.findById(id);

    // Cast the concrete event to its interface
    Optional<IEvent> interfaceEvent;
    try {
      interfaceEvent = Optional.of((IEvent) concreteEvent.get());
    } catch (NoSuchElementException e) {
      interfaceEvent = Optional.empty();
    }
    return interfaceEvent;
  }

  @Override
  public List<IEvent> findAll() {
    return jpa.findAll()
        .stream()
        .map((e) -> (IEvent) e)
        .toList();
  }
}
