package york.studentevents.events;

import java.util.List;

/**
 * Application service exposing event-related business operations.
 *
 * <p>This service forms the {@code Service} layer of the stack, sitting between the
 * {@link EventController} and the {@link IEventRepository}. Persistence is delegated to the
 * injected repository; the service holds no state of its own.
 *
 * @see IEventRepository
 * @see IEvent
 */
@Deprecated
public class EventService {

  private IEventRepository repository;

  /**
   * Constructs an {@code EventService} backed by the given repository.
   *
   * @param repositoryInjection the repository used to store and retrieve events; must not be
   *     {@code null}
   */
  @Deprecated
  public EventService(IEventRepository repositoryInjection) {
    if (repositoryInjection == null) {
      throw new IllegalArgumentException("repositoryInjection must not be null");
    }
    this.repository = repositoryInjection;
  }

  /**
   * Retrieves every event currently held by the backing repository.
   *
   * @return a {@link List} of all events; never {@code null}, but may be empty if no events have
   *     been saved
   */
  @Deprecated
  public List<IEvent> getAllEvents() {
    return repository.findAll();
  }

}
