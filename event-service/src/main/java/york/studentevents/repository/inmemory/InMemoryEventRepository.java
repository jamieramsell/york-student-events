package york.studentevents.repository.inmemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import york.studentevents.events.Event;
import york.studentevents.events.IEvent;
import york.studentevents.events.IEventRepository;

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
@Repository
public class InMemoryEventRepository implements IEventRepository {

  private Map<UUID, IEvent> hashMap;

  /**
   * Constructs a new, empty {@code InMemoryEventRepository}.
   */
  public InMemoryEventRepository() {
    hashMap = new HashMap<>();

    // Adding some hardcoded test events
    IEvent ball = new Event("Summer ball 2026", 1000, "Formal");
    hashMap.put(ball.getId(), ball);

    IEvent roses = new Event("Roses 2026", "Sport");
    hashMap.put(roses.getId(), roses);
  }

  @Override
  public void save(IEvent entity) {
    UUID id = entity.getId();
    hashMap.put(id, entity);
  }

  @Override
  public List<IEvent> findAll() {
    List<IEvent> returnList = new ArrayList<>();
    returnList.addAll(hashMap.values());
    return returnList;
  }

}
