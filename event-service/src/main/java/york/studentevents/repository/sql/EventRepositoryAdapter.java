package york.studentevents.repository.sql;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import york.studentevents.events.Event;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;

public class EventRepositoryAdapter implements IEventRepository {

  private final JpaEventRepository jpa;

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
