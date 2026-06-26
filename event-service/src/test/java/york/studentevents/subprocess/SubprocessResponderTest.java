package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link SubprocessResponder}.
 *
 * <p>The responder's {@code main} calls {@link System#exit}, so each case runs it in a fresh JVM
 * (reusing the test classpath) and asserts on the response envelope and exit code, mirroring how
 * the Python client drives it.
 */
class SubprocessResponderTest {

  private static final String KNOWN_USER = "11111111-1111-1111-1111-111111111111";
  private static final String UNKNOWN_USER = "00000000-0000-0000-0000-000000000000";

  /** The exit code and parsed response envelope from one responder invocation. */
  private record Result(int exitCode, JsonObject response) {}

  private static String request(String requestType, String userId) {
    return String.format(
        "{\"requestType\":\"%s\",\"payload\":{\"userId\":\"%s\"}}", requestType, userId);
  }

  private static Result run(String requestLine) throws Exception {
    String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    ProcessBuilder builder = new ProcessBuilder(
        javaBin,
        "-cp",
        System.getProperty("java.class.path"),
        "york.studentevents.subprocess.SubprocessResponder");

    Process process = builder.start();
    try (OutputStream stdin = process.getOutputStream()) {
      stdin.write((requestLine + "\n").getBytes(StandardCharsets.UTF_8));
    }

    String output;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      output = reader.lines().collect(Collectors.joining("\n")).trim();
    }

    int exitCode = process.waitFor();
    JsonObject response =
        output.isEmpty() ? null : JsonParser.parseString(output).getAsJsonObject();
    return new Result(exitCode, response);
  }

  private static String errorOf(Result result) {
    return result.response().get("error").getAsString();
  }

  @Test
  void knownUserReturnsCannedEventsAndExitsZero() throws Exception {
    Result result = run(request("GET_USER_EVENTS", KNOWN_USER));
    assertEquals(0, result.exitCode());
    assertEquals("ok", result.response().get("status").getAsString());
    JsonArray events = result.response().getAsJsonObject("payload").getAsJsonArray("events");
    assertEquals(2, events.size());
    assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", events.get(0).getAsString());
    assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", events.get(1).getAsString());
  }

  @Test
  void unknownUserReturnsErrorAndExitsNonZero() throws Exception {
    Result result = run(request("GET_USER_EVENTS", UNKNOWN_USER));
    assertEquals(1, result.exitCode());
    assertEquals("error", result.response().get("status").getAsString());
    assertTrue(errorOf(result).contains("not found"));
  }

  @Test
  void malformedJsonReturnsError() throws Exception {
    Result result = run("not json");
    assertEquals(1, result.exitCode());
    assertEquals("Incorrectly formatted json.", errorOf(result));
  }

  @Test
  void missingRequestTypeReturnsError() throws Exception {
    Result result = run("{\"payload\":{\"userId\":\"" + KNOWN_USER + "\"}}");
    assertEquals(1, result.exitCode());
    assertEquals("Missing 'requestType' field.", errorOf(result));
  }

  @Test
  void missingPayloadReturnsError() throws Exception {
    Result result = run("{\"requestType\":\"GET_USER_EVENTS\"}");
    assertEquals(1, result.exitCode());
    assertEquals("Missing 'payload' field.", errorOf(result));
  }

  @Test
  void unrecognisedRequestTypeReturnsError() throws Exception {
    Result result = run(request("NOPE", KNOWN_USER));
    assertEquals(1, result.exitCode());
    assertEquals("'requestType' field is not valid.", errorOf(result));
  }

  @Test
  void unsupportedRequestTypeReturnsError() throws Exception {
    Result result = run(request("GET_USER_BADGES", KNOWN_USER));
    assertEquals(1, result.exitCode());
    assertTrue(errorOf(result).contains("Unsupported requestType"));
  }

  @Test
  void missingUserIdReturnsError() throws Exception {
    Result result = run("{\"requestType\":\"GET_USER_EVENTS\",\"payload\":{}}");
    assertEquals(1, result.exitCode());
    assertEquals("Missing 'userId' field.", errorOf(result));
  }

  @Test
  void invalidUserIdReturnsError() throws Exception {
    Result result = run(request("GET_USER_EVENTS", "not-a-uuid"));
    assertEquals(1, result.exitCode());
    assertEquals("'userId' field is not a valid UUID.", errorOf(result));
  }
}
