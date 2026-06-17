package york.studentevents.repository.inmemory;

import york.studentevents.events.IEventRepository;
import york.studentevents.events.Event;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * In-memory implementation of {@link IEventRepository}.
 *
 * <p>Events are stored in a {@link HashMap} keyed by each event's {@link UUID}, so saving an event
 * whose ID is already present overwrites the existing entry.
 *
 * <p>This implementation holds no persistent state; all data is lost when the application stops. It
 * is intended for development and testing only.
 *
 * @see IEventRepository
 */
public class InMemoryEventRepository implements IEventRepository {

  private Map<UUID, Event> hashMap;

  /**
   * Constructs a new, empty {@code InMemoryEventRepository}.
   */
  public InMemoryEventRepository() {
    hashMap = new HashMap<>();
  }

  @Override
  public void save(Event entity) {
    UUID id = entity.getId();
    hashMap.put(id, entity);
  }

  @Override
  public List<Event> findAll() {
    List<Event> returnList = new ArrayList<>();
    returnList.addAll(hashMap.values());
    return returnList;
  }

}
