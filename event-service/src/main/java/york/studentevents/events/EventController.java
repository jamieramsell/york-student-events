package york.studentevents.events;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing event data over HTTP.
 *
 * <p>This controller is intentionally thin: it contains no business logic and delegates entirely to
 * {@link EventService}. Its only responsibility is to map incoming HTTP requests to the appropriate
 * service method and return the result, which Spring serialises to JSON.
 */
@RestController
public class EventController {
  
  private final EventService eventService;

  /**
   * Constructs an {@code EventController} with the service it delegates to.
   *
   * <p>The {@link EventService} is injected by Spring via constructor injection, rather than being
   * instantiated directly, keeping the controller decoupled from how the service is constructed.
   *
   * @param service the service used to retrieve event data; must not be {@code null}
   */
  public EventController(EventService service) {
    if (service == null) {
      throw new IllegalArgumentException("service cannot be null");
    }
    this.eventService = service;
  }

  /**
   * Handles {@code GET /events} HTTP requests and returns all events.
   *
   * <p>Returns an empty list (serialised as an empty JSON array) when no events exist, rather than
   * an error. A successful call responds with HTTP 200.
   *
   * @return a list of all events, serialised by Spring into a JSON array
   */
  @GetMapping("/events")
  public List<IEvent> getAllEvents() {
    return eventService.getAllEvents();
  }

}
