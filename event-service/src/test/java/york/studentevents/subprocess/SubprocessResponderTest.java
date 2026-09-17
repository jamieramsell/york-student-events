package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Envelope-shape integration tests for {@link SubprocessResponder}.
 *
 * <p>The responder's {@code main} calls {@link System#exit}, so each case runs it in a fresh JVM
 * (reusing the test classpath) and asserts on the response envelope and exit code, mirroring
 * how the Python client drives it.
 *
 * <p>Each spawned process is launched with {@code YSE_BRIDGE_INMEMORY} set, so it composes a
 * throwaway in-memory graph and needs no database. That graph starts <em>empty</em> and dies with
 * the process, and a cross-JVM child cannot see data seeded in this test's JVM — so these tests
 * assert only envelope <em>shapes</em> ({@code ok} / {@code error} / exit code) and the
 * not-found and validation paths. The real-data assertions (that a seeded event yields its
 * actual host, start and
 * category) live in {@link SubprocessResponderInProcessTest}, which drives the handlers in-process
 * over a seeded database.
 */
class SubprocessResponderTest {

  private static final String SOME_USER = "11111111-1111-1111-1111-111111111111";
  private static final String SOME_EVENT = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

  /** The exit code and parsed response envelope from one responder invocation. */
  private record Result(int exitCode, JsonObject response) {}

  private static String request(String requestType, String userId) {
    return String.format(
        "{\"requestType\":\"%s\",\"payload\":{\"userId\":\"%s\"}}", requestType, userId);
  }

  private static String eventRequest(String requestType, String eventId) {
    return String.format(
        "{\"requestType\":\"%s\",\"payload\":{\"eventId\":\"%s\"}}", requestType, eventId);
  }

  private static String batchRequest(String... eventIds) {
    StringBuilder ids = new StringBuilder();
    for (int i = 0; i < eventIds.length; i++) {
      if (i > 0) {
        ids.append(",");
      }
      ids.append("\"").append(eventIds[i]).append("\"");
    }
    return String.format(
        "{\"requestType\":\"GET_BATCH_EVENT_INFO\",\"payload\":{\"eventIds\":[%s]}}", ids);
  }

  private static String badgeAwardedRequest(String userId, String badgeName) {
    return String.format(
        "{\"requestType\":\"BADGE_AWARDED\",\"payload\":{\"userId\":\"%s\",\"badgeName\":\"%s\"}}",
        userId, badgeName);
  }

  private static Result run(String requestLine) throws Exception {
    String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    ProcessBuilder builder = new ProcessBuilder(
        javaBin,
        "-cp",
        System.getProperty("java.class.path"),
        "york.studentevents.subprocess.SubprocessResponder");
    // Compose a database-free in-memory graph, so the spawned process needs no DATABASE_URL.
    builder.environment().put("YSE_BRIDGE_INMEMORY", "1");

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

  // ok-shape: a request needing no data succeeds //

  @Test
  void batchOfNoEventsReturnsEmptyObjectAndExitsZero() throws Exception {
    Result result = run(batchRequest());
    assertEquals(0, result.exitCode());
    assertEquals("ok", result.response().get("status").getAsString());
    JsonObject events = result.response().getAsJsonObject("payload").getAsJsonObject("events");
    assertEquals(0, events.size());
  }

  // not-found: every id is unknown in the empty graph //

  @Test
  void unknownUserReturnsErrorAndExitsNonZero() throws Exception {
    Result result = run(request("GET_USER_EVENTS", SOME_USER));
    assertEquals(1, result.exitCode());
    assertEquals("error", result.response().get("status").getAsString());
    assertTrue(errorOf(result).contains("not recognised"));
  }

  @Test
  void unknownEventReturnsErrorAndExitsNonZero() throws Exception {
    Result result = run(eventRequest("GET_EVENT_INFO", SOME_EVENT));
    assertEquals(1, result.exitCode());
    assertEquals("error", result.response().get("status").getAsString());
    assertTrue(errorOf(result).contains("not recognised"));
  }

  @Test
  void batchWithUnknownEventReturnsErrorAndExitsNonZero() throws Exception {
    Result result = run(batchRequest(SOME_EVENT));
    assertEquals(1, result.exitCode());
    assertEquals("error", result.response().get("status").getAsString());
    assertTrue(errorOf(result).contains("not recognised"));
  }

  @Test
  void unknownUserBadgeAwardedReturnsErrorAndExitsNonZero() throws Exception {
    Result result = run(badgeAwardedRequest(SOME_USER, "First Event"));
    assertEquals(1, result.exitCode());
    assertEquals("error", result.response().get("status").getAsString());
    assertTrue(errorOf(result).contains("not recognised"));
  }

  // validation errors: rejected before any context is booted //

  @Test
  void missingEventIdReturnsError() throws Exception {
    Result result = run("{\"requestType\":\"GET_EVENT_INFO\",\"payload\":{}}");
    assertEquals(1, result.exitCode());
    assertEquals("Missing 'eventId' field.", errorOf(result));
  }

  @Test
  void invalidEventIdReturnsError() throws Exception {
    Result result = run(eventRequest("GET_EVENT_INFO", "not-a-uuid"));
    assertEquals(1, result.exitCode());
    assertEquals("'eventId' field is not a valid UUID.", errorOf(result));
  }

  @Test
  void missingEventIdsReturnsError() throws Exception {
    Result result = run("{\"requestType\":\"GET_BATCH_EVENT_INFO\",\"payload\":{}}");
    assertEquals(1, result.exitCode());
    assertEquals("Missing 'eventIds' field.", errorOf(result));
  }

  @Test
  void eventIdsNotAnArrayReturnsError() throws Exception {
    Result result =
        run("{\"requestType\":\"GET_BATCH_EVENT_INFO\",\"payload\":{\"eventIds\":\"" + SOME_EVENT
            + "\"}}");
    assertEquals(1, result.exitCode());
    assertEquals("'eventIds' field is not valid.", errorOf(result));
  }

  @Test
  void batchWithInvalidUuidReturnsError() throws Exception {
    Result result = run(batchRequest(SOME_EVENT, "not-a-uuid"));
    assertEquals(1, result.exitCode());
    assertEquals("'eventIds' field contains an ID which is not a valid UUID.", errorOf(result));
  }

  @Test
  void missingBadgeNameReturnsError() throws Exception {
    Result result = run(
        "{\"requestType\":\"BADGE_AWARDED\",\"payload\":{\"userId\":\"" + SOME_USER + "\"}}");
    assertEquals(1, result.exitCode());
    assertEquals("Missing 'badgeName' field.", errorOf(result));
  }

  @Test
  void blankBadgeNameReturnsError() throws Exception {
    Result result = run(badgeAwardedRequest(SOME_USER, "   "));
    assertEquals(1, result.exitCode());
    assertEquals("'badgeName' field is not valid.", errorOf(result));
  }

  @Test
  void malformedJsonReturnsError() throws Exception {
    Result result = run("not json");
    assertEquals(1, result.exitCode());
    assertEquals("Incorrectly formatted json.", errorOf(result));
  }

  @Test
  void missingRequestTypeReturnsError() throws Exception {
    Result result = run("{\"payload\":{\"userId\":\"" + SOME_USER + "\"}}");
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
    Result result = run(request("NOPE", SOME_USER));
    assertEquals(1, result.exitCode());
    assertEquals("'requestType' field is not valid.", errorOf(result));
  }

  @Test
  void unsupportedRequestTypeReturnsError() throws Exception {
    Result result = run(request("GET_USER_BADGES", SOME_USER));
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
  void blankInputReturnsError() throws Exception {
    Result result = run("");
    assertEquals(1, result.exitCode());
    assertEquals("No request received on standard input.", errorOf(result));
  }

  @Test
  void invalidUserIdReturnsError() throws Exception {
    Result result = run(request("GET_USER_EVENTS", "not-a-uuid"));
    assertEquals(1, result.exitCode());
    assertEquals("'userId' field is not a valid UUID.", errorOf(result));
  }
}
