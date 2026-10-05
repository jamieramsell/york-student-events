package york.studentevents.events;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import york.studentevents.events.dto.CreateEventDTO;
import york.studentevents.events.dto.EventDTO;
import york.studentevents.events.dto.PatchEventDTO;
import york.studentevents.exceptions.EventNotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing event data over HTTP.
 *
 * <p>This controller is intentionally thin: it contains no business logic and delegates entirely to
 * {@link EventService}. Its only responsibility is to map incoming HTTP requests to the appropriate
 * service method and return the result, which Spring serialises to JSON.
 */
@RestController
@RequestMapping("${api.base-path}/events")
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
  @GetMapping
  public List<EventDTO> getAllEvents() {
    return eventService.getAllEvents().stream()
            .map(EventDTO::fromEntity)
            .toList();
  }


  @GetMapping("/{eventId}")
  public ResponseEntity<EventDTO> getEvent(@PathVariable UUID eventId) {
    return ResponseEntity.ok(EventDTO.fromEntity(eventService.getEvent(eventId)));
  }

  /**
   * Handles {@code POST /events} HTTP requests to create a new event.
   *
   * <p>The request body must contain a valid {@link CreateEventDTO} instance.
   *
   * <p>If the event is successfully created, the response contains the created event, serialised.
   *
   * @param dto Thw DTO containing the event data.
   * @return The created event, serialised by Spring into a JSON object.
   */
  @PostMapping
  public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody CreateEventDTO dto) {
    return ResponseEntity.status(201).body(
        EventDTO.fromEntity(eventService.createEvent(dto.title(), dto.category(), dto.capacity()))
    );
  }

  @PatchMapping("/{eventId}")
  public ResponseEntity<EventDTO> updateEvent(@Valid @RequestBody PatchEventDTO dto, @PathVariable UUID eventId) {
    boolean changed = false;
    if (dto.title() != null) {
      eventService.updateEventTitle(eventId, dto.title());
      changed = true;
    }
    if (dto.description() != null) {
      eventService.updateEventDescription(eventId, dto.description());
      changed = true;
    }
    if (dto.capacity() != null) {
      eventService.updateEventCapacity(eventId, dto.capacity());
      changed = true;
    }
    if (dto.startDateTime() != null && dto.endDateTime() != null) {
      eventService.updateEventDateTime(eventId, dto.startDateTime(), dto.endDateTime());
      changed = true;
    }
    if (dto.venueId() != null) {
      eventService.updateEventVenue(eventId, dto.venueId());
      changed = true;
    }
    if (dto.category() != null) {
      eventService.updateEventCategory(eventId, dto.category());
      changed = true;
    }
    if (changed) {
      return ResponseEntity.ok(EventDTO.fromEntity(eventService.getEvent(eventId)));
    } else {
      return ResponseEntity.noContent().build();
    }
  }

  /**
   * Handles {@code DELETE /events/{eventId}} HTTP requests to delete a specific event by its ID.
   *
   * <p>If the event exists, the deletion is processed, and the response is empty
   * with HTTP status 204 (No Content).
   *
   * @param eventId the unique identifier of the event to delete; must not be null.
   * @return a {@code ResponseEntity} with HTTP status 204 if the deletion is successful.
   */
  @DeleteMapping("/{eventId}")
  public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
    eventService.deleteEvent(eventId);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(EventNotFoundException.class)
  public ResponseEntity<String> handleEventNotFound(EventNotFoundException e) {
    return ResponseEntity.status(404).body("Invalid event ID");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }
}

