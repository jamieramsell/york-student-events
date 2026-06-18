package york.studentevents.events;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {
  
  private final EventService eventService;

  public EventController(EventService service) {
    this.eventService = service;
  }

  @GetMapping("/events") // Map method to HTTP 'GET /events' request
  public List<IEvent> getAllEvents() {
    return eventService.getAllEvents();
  }

}
