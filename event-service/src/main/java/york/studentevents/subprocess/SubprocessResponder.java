package york.studentevents.subprocess;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import york.studentevents.events.StudentEventService;

/**
 * Standalone entry point that answers subprocess requests issued by the Python {@code api-core}
 * service.
 *
 * <p>This is the structural mirror of {@code api-core/src/bridge/responder.py}, but for the
 * opposite direction of the bridge. Where {@link SubprocessRequestFactory} has
 * {@code event-service} spawn Python and read its response, here {@code api-core} spawns this
 * responder as a fresh process, writes a JSON request envelope to its standard input, and reads a
 * JSON response envelope from its standard output.
 *
 * <p>Each invocation reads a single request envelope, routes on its {@link RequestType}, and emits
 * a response envelope. Both follow the shared JSON contract documented in
 * {@code docs/subprocess-contract.md}. A malformed request, an unknown or unsupported request
 * type, or an unknown user yields an {@code error} envelope and a non-zero exit status.
 *
 * <p>Only {@link RequestType#GET_USER_EVENTS}, {@link RequestType#GET_EVENT_INFO} and
 * {@link RequestType#BADGE_AWARDED} are currently supported: {@code event-service} owns event data
 * and is the party notified when {@code api-core} auto-awards a badge; badge and friend queries
 * belong to {@code api-core}.
 *
 * @see RequestType
 * @see SubprocessRequestFactory
 */
public class SubprocessResponder {

  private static final Gson GSON = new Gson();

  /** Sentinel user recognised by this canned-stub responder. */
  private static final UUID KNOWN_USER_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  /** Canned event IDs returned for the known user (stands in for a real EventService query). */
  private static final List<UUID> CANNED_EVENTS = List.of(
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

  /** Canned event info keyed by event ID (stands in for a real EventService query). */
  private static final Map<UUID, EventInfoPayload> CANNED_EVENT_INFO = Map.of(
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
      new EventInfoPayload(
          UUID.fromString("22222222-2222-2222-2222-222222222222"),
          "2026-09-15T18:00:00",
          "SOCIAL"),
      UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
      new EventInfoPayload(
          UUID.fromString("33333333-3333-3333-3333-333333333333"),
          "2026-10-01T14:30:00",
          "ACADEMIC"));

  /** Request envelope: a request type and its still-raw JSON payload. */
  private record RequestEnvelope(RequestType requestType, JsonObject payload) {}

  /** Response envelope for a successful request; {@code payload} shape depends on the request. */
  private record OkResponse(String status, Object payload) {}

  /** Payload of a successful {@code GET_USER_EVENTS} response. */
  private record EventsPayload(List<UUID> events) {}

  /** Payload of a successful {@code GET_EVENT_INFO} response. */
  private record EventInfoPayload(UUID host, String start, String category) {}

  /** Empty payload for a successful acknowledgement (e.g. a {@code BADGE_AWARDED} response). */
  private record EmptyPayload() {}

  /** Response envelope for a failed request. */
  private record ErrorResponse(String status, String error) {}

  /**
   * Entry point: answers a single subprocess request read from standard input and writes the
   * response to standard output.
   *
   * <p>Reads one JSON request envelope, deserialises and validates it, dispatches it on its
   * {@link RequestType}, and writes the resulting {@code ok} response envelope. On success the
   * process exits normally (zero).
   *
   * <p>Any failure (a malformed request, an unknown or unsupported request type, an unknown user,
   * or an unexpected error such as an I/O failure on standard input) is caught here, written to
   * standard output as an {@code error} envelope, and terminated with a non-zero exit status. No
   * exception is allowed to propagate out of this method.
   *
   * @param args command-line arguments; unused.
   */
  public static void main(String[] args) {
    try {
      String requestJson = readRequest();
      RequestEnvelope envelope = deserialiseEnvelope(requestJson);
      writeResponse(route(envelope));
    } catch (IllegalArgumentException e) {
      // Bad data: malformed request, unknown/unsupported type, or unknown user.
      writeResponse(GSON.toJson(new ErrorResponse("error", e.getMessage())));
      System.exit(1);
    } catch (Exception e) {
      // Anything unexpected (e.g. an I/O failure reading stdin).
      writeResponse(GSON.toJson(new ErrorResponse("error", "Unexpected error: " + e.getMessage())));
      System.exit(1);
    }
  }

  /**
   * Reads the single request envelope line from standard input.
   *
   * @return the raw JSON request line.
   *
   * @throws IllegalArgumentException if no input is received.
   * @throws java.io.IOException if standard input cannot be read.
   */
  private static String readRequest() throws java.io.IOException {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    String line = reader.readLine();
    if (line == null || line.isBlank()) {
      throw new IllegalArgumentException("No request received on standard input.");
    }
    return line;
  }

  /**
   * Deserialises a raw JSON request envelope into a typed {@link RequestEnvelope}.
   *
   * <p>The supplied string must be a single JSON object containing a {@code requestType} field
   * (one of {@link RequestType}) and a {@code payload} object.
   *
   * @param json the raw JSON request envelope, as received on standard input; expected to be a
   *     single JSON object.
   * @return the deserialised {@link RequestEnvelope} holding the request type and its payload.
   *
   * @throws IllegalArgumentException if {@code json} is not a valid JSON object; if the
   *     {@code requestType} or {@code payload} fields are missing or malformed.
   */
  private static RequestEnvelope deserialiseEnvelope(String json) {

    /*
     * Try to parse the given String into a JsonObject. If this throws an error, then the String
     * provided is either not valid JSON, or is valid JSON, but is not an Object (e.g. an array or a
     * string).
     */
    JsonObject envelope;
    try {
      envelope = JsonParser.parseString(json).getAsJsonObject();
    } catch (JsonSyntaxException | IllegalStateException e) {
      throw new IllegalArgumentException("Incorrectly formatted json.");
    }

    // Check the structure of the envelope is valid
    validateEnvelope(envelope);

    // Create the envelope object and return it
    RequestType requestType = getRequestType(envelope);
    JsonObject payload = envelope.getAsJsonObject("payload");

    RequestEnvelope requestEnvelope = new RequestEnvelope(requestType, payload);

    return requestEnvelope;

  }

  /**
   * Convenience function which checks whether the structure of a given JsonObject matches that of a
   * valid request envelope.
   *
   * @param envelope The JsonObject to check.
   *
   * @throws IllegalArgumentException if the envelope is missing a requestType or payload field.
   */
  private static void validateEnvelope(JsonObject envelope) {
    // Checks that the JsonObject has a 'requestType' field, which is a primitive element (String)
    if (!envelope.has("requestType") || !envelope.get("requestType").isJsonPrimitive()) {
      throw new IllegalArgumentException("Missing 'requestType' field.");
    }

    // Checks that the JsonObject has a 'payload' field, which itself is another JsonObject
    if (!envelope.has("payload") || !envelope.get("payload").isJsonObject()) {
      throw new IllegalArgumentException("Missing 'payload' field.");
    }
  }

  /**
   * Convenience function which retrieves the RequestType from a given envelope.
   *
   * <p>Note that the value stored at the field 'requestType' must be one of {@link RequestType}.
   *
   * @param envelope the request envelope from which to retrieve the RequestType.
   * @return The type of request
   *
   * @throws IllegalArgumentException if the value of requestType is not a value of the RequestType
   *     enum.
   */
  private static RequestType getRequestType(JsonObject envelope) {
    RequestType type = GSON.fromJson(envelope.get("requestType"), RequestType.class);
    if (type == null) {
      throw new IllegalArgumentException("'requestType' field is not valid.");
    }

    return type;
  }

  /**
   * Routes a deserialised request to its handler and returns the JSON response envelope.
   *
   * @param envelope the deserialised request.
   * @return the JSON {@code ok} response envelope.
   *
   * @throws IllegalArgumentException if the request type is not supported by this responder; or a
   *     required attribute is missing from the payload or is not valid.
   */
  private static String route(RequestEnvelope envelope) {

    switch (envelope.requestType()) {
      case GET_USER_EVENTS -> {
        UUID userId = getUserId(envelope.payload());
        return getUserEvents(userId);
      }
      case BADGE_AWARDED -> {
        UUID userId = getUserId(envelope.payload());
        String badgeName = getBadgeName(envelope.payload());
        return badgeAwarded(userId, badgeName);
      }
      case GET_EVENT_INFO -> {
        UUID eventId = getEventId(envelope.payload());
        return getEventInfo(eventId);
      }
      default -> throw new IllegalArgumentException(
          "Unsupported requestType for event-service: " + envelope.requestType());
    }
  }

  /**
   * Convenience function which retrieves the user's ID from a request payload.
   *
   * @param payload The request payload from which to retrieve the target user's ID
   * @return The UUID of the target user.
   *
   * @throws IllegalArgumentException if the payload is missing a userId field, or the userId field
   *     is not valid.
   *
   * @see #validateEnvelope(com.google.gson.JsonObject)
   */
  private static UUID getUserId(JsonObject payload) {
    // Checks that the payload contains a value named 'userId', which is not null.
    if (!payload.has("userId") || payload.get("userId").isJsonNull()) {
      throw new IllegalArgumentException("Missing 'userId' field.");
    }

    // Try to parse the userId element of the payload into a String
    String userId;
    try {
      userId = payload.get("userId").getAsString();
    } catch (UnsupportedOperationException | IllegalStateException e) {
      throw new IllegalArgumentException("'userId' field is not valid.");
    }

    // Try to parse the userId String into a UUID
    UUID uuidUserId;
    try {
      uuidUserId = UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("'userId' field is not a valid UUID.");
    }

    return uuidUserId;
  }

  /**
   * Convenience function which retrieves the event's ID from a request payload.
   *
   * @param payload The request payload from which to retrieve the target event's ID
   * @return The UUID of the target event.
   *
   * @throws IllegalArgumentException if the payload is missing an eventId field, or the eventId
   *     field is not valid.
   *
   * @see #validateEnvelope(com.google.gson.JsonObject)
   */
  private static UUID getEventId(JsonObject payload) {
    // Checks that the payload contains a value named 'eventId', which is not null.
    if (!payload.has("eventId") || payload.get("eventId").isJsonNull()) {
      throw new IllegalArgumentException("Missing 'eventId' field.");
    }

    // Try to parse the eventId element of the payload into a String
    String eventId;
    try {
      eventId = payload.get("eventId").getAsString();
    } catch (UnsupportedOperationException | IllegalStateException e) {
      throw new IllegalArgumentException("'eventId' field is not valid.");
    }

    // Try to parse the eventId String into a UUID
    UUID uuidEventId;
    try {
      uuidEventId = UUID.fromString(eventId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("'eventId' field is not a valid UUID.");
    }

    return uuidEventId;
  }

  /**
   * Convenience function which retrieves the badge's display name from a request payload.
   *
   * @param payload The request payload from which to retrieve the awarded badge's name.
   * @return The display name of the badge.
   *
   * @throws IllegalArgumentException if the payload is missing a badgeName field, or the badgeName
   *     field is not a non-blank string.
   *
   * @see #validateEnvelope(com.google.gson.JsonObject)
   */
  private static String getBadgeName(JsonObject payload) {
    // Checks that the payload contains a value named 'badgeName', which is not null.
    if (!payload.has("badgeName") || payload.get("badgeName").isJsonNull()) {
      throw new IllegalArgumentException("Missing 'badgeName' field.");
    }

    // Try to parse the badgeName element of the payload into a String
    String badgeName;
    try {
      badgeName = payload.get("badgeName").getAsString();
    } catch (UnsupportedOperationException | IllegalStateException e) {
      throw new IllegalArgumentException("'badgeName' field is not valid.");
    }

    // A badge always has a non-blank display name; reject anything blank as malformed.
    if (badgeName.isBlank()) {
      throw new IllegalArgumentException("'badgeName' field is not valid.");
    }

    return badgeName;
  }

  /**
   * Returns the events for the given user as a JSON {@code ok} response envelope.
   *
   * <p>Note that this method is currently just a stub, retrieving hardcoded data for testing
   *     purposes. Logic must be ripped out and swapped for code which contacts
   *     {@link StudentEventService} when persistence is configured.
   *
   * @param userId the user whose events to return.
   * @return the JSON {@code ok} response envelope.
   *
   * @throws IllegalArgumentException if the user ID is not recognised.
   */
  private static String getUserEvents(UUID userId) {
    if (!KNOWN_USER_ID.equals(userId)) {
      throw new IllegalArgumentException("User " + userId + " not found");
    }
    return GSON.toJson(new OkResponse("ok", new EventsPayload(CANNED_EVENTS)));
  }

  /**
   * Returns the information required about a given events as a JSON {@code ok} response envelope.
   *
   * <p>Note that this method is currently just a stub, retrieving hardcoded data for testing
   *     purposes. Logic must be ripped out and swapped for code which contacts
   *     {@link StudentEventService} when persistence is configured.
   *
   * @param eventId the event to retrieve info about.
   * @return the JSON {@code ok} response envelope.
   *
   * @throws IllegalArgumentException if the event ID is not recognised.
   */
  private static String getEventInfo(UUID eventId) {
    EventInfoPayload info = CANNED_EVENT_INFO.get(eventId);
    if (info == null) {
      throw new IllegalArgumentException("Event " + eventId + " not found");
    }
    return GSON.toJson(new OkResponse("ok", info));
  }

  /**
   * Notifies the given user that they have been awarded a badge, returning an empty JSON
   * {@code ok} response envelope.
   *
   * <p>Note that this method is currently just a stub: it validates the request and acknowledges
   * it, but does not yet act on the notification. A real implementation would deliver the award to
   * the user (e.g. as a user-facing notification) once persistence and dependency injection are
   * configured.
   *
   * @param userId the user who has been awarded the badge.
   * @param badgeName the display name of the badge that was awarded.
   * @return the JSON {@code ok} response envelope.
   *
   * @throws IllegalArgumentException if the user ID is not recognised.
   */
  private static String badgeAwarded(UUID userId, String badgeName) {
    if (!KNOWN_USER_ID.equals(userId)) {
      throw new IllegalArgumentException("User " + userId + " not found");
    }
    return GSON.toJson(new OkResponse("ok", new EmptyPayload()));
  }

  /** Writes a response envelope to standard output, newline-terminated and flushed. */
  private static void writeResponse(String json) {
    System.out.println(json);
    System.out.flush();
  }

}