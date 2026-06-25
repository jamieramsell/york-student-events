package york.studentevents.subprocess;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.UUID;

/**
 * Standalone entry point that answers subprocess requests issued by the Python {@code api-core}
 * service.
 *
 * <p>This is the structural mirror of {@code api-core/src/subprocess_bridge.py}, but for the
 * opposite direction of the bridge. Where {@link SubprocessRequestFactory} has
 * {@code event-service} spawn Python and read its response, here {@code api-core} spawns this
 * responder as a fresh process, writes a JSON request envelope to its standard input, and reads a
 * JSON response envelope from its standard output.
 *
 * <p>Each invocation reads a single request envelope, routes on its {@link RequestType}, and emits
 * a response envelope. Both follow the shared JSON contract documented in
 * {@code docs/docs/subprocess-contract.md}. A malformed request, an unknown or unsupported request
 * type, or an unknown user yields an {@code error} envelope and a non-zero exit status.
 *
 * <p>Only {@link RequestType#GET_USER_EVENTS} is currently supported, since {@code event-service}
 * owns event data; badge and friend requests belong to {@code api-core}.
 *
 * @see RequestType
 * @see SubprocessRequestFactory
 */
public class SubprocessResponder {

  private static record RequestEnvelope(RequestType requestType, Payload payload) {}

  private static final Gson GSON = new Gson();

  /**
   * Deserialises a raw JSON request envelope into a typed {@link RequestEnvelope}.
   *
   * <p>The supplied string must be a single JSON object containing a {@code requestType} field
   * (one of {@link RequestType}) and a {@code payload} object. The payload is converted into the
   * {@link Payload} implementation appropriate to the request type; for {@code GET_USER_EVENTS}
   * this is a {@link UserIdPayload} built from the payload's {@code userId}.
   *
   * @param json the raw JSON request envelope, as received on standard input; expected to be a
   *     single JSON object.
   * @return the deserialised {@link RequestEnvelope} holding the request type and its payload.
   *
   * @throws IllegalArgumentException if {@code json} is not a valid JSON object; if the
   *     {@code requestType} or {@code payload} fields are missing or malformed; if
   *     {@code requestType} is not a recognised {@link RequestType} or is one this responder does
   *     not support; or if {@code userId} is missing or not a valid UUID.
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

    /*
     * Try to parse the payload element of root into a JsonObject. If this throws an error, then the
     * element named 'payload' is not an Object.
     */
    JsonObject payload;
    try {
      payload = envelope.getAsJsonObject("payload");
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("'payload' field is not valid.");
    }

    // Create the envelope object and return it
    UUID userId = getUserId(payload);
    RequestType requestType = getRequestType(envelope);

    Payload requestPayload = switch (requestType) {
      case GET_USER_EVENTS -> new UserIdPayload(userId);
      default -> throw new IllegalArgumentException("Unsupported requestType for event-service: "
          + requestType);
    };

    RequestEnvelope requestEnvelope = new RequestEnvelope(requestType, requestPayload);
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
   * Convenience function which retrieves the user's ID from a request payload.
   *
   * @param payload The request payload from which to retrieve the target user's ID
   * @return The UUID of the target user.
   *
   * @throws IllegalArgumentException if the envelope is missing a userId field, or the userId field
   *     is not valid.
   *
   * @see validateEnvelope
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

  public static void main(String[] args) {
    
  }
}
