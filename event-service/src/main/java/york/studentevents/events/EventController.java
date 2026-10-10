package york.studentevents.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import york.studentevents.events.dto.CreateEventDto;
import york.studentevents.events.dto.EventDto;
import york.studentevents.events.dto.PatchEventDto;
import york.studentevents.exceptions.EventNotFoundException;

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
   * Handles {@code GET /events} HTTP requests and returns a {@see Page} of {@see EventDto}.
   *
   * <p>Returns an empty {@see Page} if no events exist, rather than an error. A successful call
   * responds with HTTP 200.
   *
   * <p>Responds with HTTP 400 if the page number is less than 0, or the page size is less than 1.
   *
   * @param page the page number to retrieve; must be greater than or equal to 0.
   * @param pageSize the number of events to retrieve per page; must be greater than 0,
   * @return a {@code List} of all events, serialised by Spring into a JSON array
   */
  @GetMapping
  public Page<EventDto> getAllEvents(
      @Valid @PathVariable @Min(value = 0) int page,
      @Valid @PathVariable @Min(value = 1) @Max(value = 100) int pageSize) {
    return eventService.getAllEvents(page, pageSize).map(EventDto::fromEntity);
  }

  /**
   * Handles {@code GET /events/{eventId}} HTTP requests and returns a specific event by its ID.
   *
   * <p>Returns a 404 (Not Found) response if the event does not exist.
   *
   * @param eventId The ID of the event to retrieve.
   * @return A {@code ResponseEntity} containing the event, serialised by Spring into a JSON object.
   */
  @GetMapping("/{eventId}")
  public ResponseEntity<EventDto> getEvent(@PathVariable UUID eventId) {
    return ResponseEntity.ok(EventDto.fromEntity(eventService.getEvent(eventId)));
  }

  /**
   * Handles {@code POST /events} HTTP requests to create a new event.
   *
   * <p>The request body must contain a valid {@link CreateEventDto} instance.
   *
   * <p>If the event is successfully created, the response contains the created event, serialised.
   * Otherwise, a 400 (Bad Request) response is returned.
   *
   * @param dto A {@code CreateEventDto} containing the details of the event to create.
   * @return The created event, serialised by Spring into a JSON object.
   */
  @PostMapping
  public ResponseEntity<EventDto> createEvent(
      @Valid @RequestBody CreateEventDto dto
  ) {
    return ResponseEntity.status(201).body(
        EventDto.fromEntity(eventService.createEvent(dto.title(), dto.category(), dto.capacity()))
    );
  }

  /**
   * Handles {@code PATCH /events/{eventId}} HTTP requests to update a specific event by its ID.
   *
   * <p>The request body must contain a valid {@link PatchEventDto} instance.
   * Missing fields and null values are not updated.
   *
   * <p>If the event exists and any of the fields are updated, the response contains the updated.
   * Otherwise, a 204 (No Content) response is returned.
   *
   * @param dto A {@code PatchEventDto} containing the fields to update.
   * @param eventId The ID of the event to update.
   * @return The updated event, serialised by Spring into a JSON object.
   */
  @PatchMapping("/{eventId}")
  public ResponseEntity<EventDto> updateEvent(
      @Valid @RequestBody PatchEventDto dto, @PathVariable UUID eventId
  ) {
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
      return ResponseEntity.ok(EventDto.fromEntity(eventService.getEvent(eventId)));
    } else {
      return ResponseEntity.noContent().build();
    }
  }

  /**
   * Handles {@code DELETE /events/{eventId}} HTTP requests to delete a specific event by its ID.
   *
   * <p>If the event exists, the deletion is processed, and the response is empty
   * with HTTP status 204 (No Content). Otherwise, a 404 (Not Found) response is returned.
   *
   * @param eventId the unique identifier of the event to delete; must not be null.
   * @return a {@code ResponseEntity} with HTTP status 204 if the deletion is successful.
   *        Otherwise, a 404 (Not Found) response is returned.
   */
  @DeleteMapping("/{eventId}")
  public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
    eventService.deleteEvent(eventId);
    return ResponseEntity.noContent().build();
  }


  /**
   * Handles exceptions of type {@code EventNotFoundException} thrown when a requested
   * event cannot be found.
   *
   * <p>Returns an HTTP 404 (Not Found) response with a message indicating an invalid event ID.
   *
   * @param e the exception object representing the event not found error
   * @return a {@code ResponseEntity} containing a message and the HTTP 404 status code
   */
  @ExceptionHandler(EventNotFoundException.class)
  public ResponseEntity<String> handleEventNotFound(EventNotFoundException e) {
    return ResponseEntity.status(404).body("Invalid event ID");
  }

  /**
   * Handles exceptions of type {@code IllegalArgumentException} thrown when an illegal argument
   * is provided in a request.
   *
   * @param e the exception object representing the illegal argument error
   * @return a {@code ResponseEntity} containing the exception message with the HTTP 400
   *        (Bad Request) status code
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }
}

