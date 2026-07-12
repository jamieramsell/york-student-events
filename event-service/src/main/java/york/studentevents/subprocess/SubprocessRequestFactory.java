package york.studentevents.subprocess;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Builds JSON requests for the subprocess and sends them, returning the subprocess's response. */
class SubprocessRequestFactory {

  /**
   * Request class for the subprocess communication.
   *
   * @param <T> the type of the payload
   * @param requestType the type of the request
   * @param payload the payload of the request
   *
   * @see UserIdPayload
   * @see AwardBadgePayload
   * @see AttendancePayload
   * @see RequestType
   */
  static record Request<T extends IPayload>(RequestType requestType, T payload) {}

  private static final Gson GSON = new Gson();
  private static final String BRIDGE_SCRIPT = "api-core/src/bridge/responder.py";
  
  /**
   * Resolves the absolute path to the Python bridge script.
   *
   * <p>If the {@code PROJECT_ROOT} environment variable or the {@code project.root} system property
   * is set, the script is anchored to that root. Otherwise the working directory ({@code user.dir})
   * and its ancestors are searched for the script, so it is found regardless of where within the
   * checkout the JVM was launched. Explicit configuration always takes precedence, and a
   * misconfigured root still fails rather than being silently discovered.
   *
   * @return the absolute, normalised path to {@code responder.py}.
   *
   * @throws UncheckedIOException if the script cannot be found.
   */
  private static Path resolveScriptPath() {
    String configuredRoot = System.getenv("PROJECT_ROOT");
    if (configuredRoot == null || configuredRoot.isBlank()) {
      configuredRoot = System.getProperty("project.root");
    }

    Path scriptPath = (configuredRoot != null && !configuredRoot.isBlank())
        ? Path.of(configuredRoot, BRIDGE_SCRIPT).toAbsolutePath().normalize()
        : discoverScriptPath();

    if (scriptPath == null || !Files.exists(scriptPath)) {
      throw new UncheckedIOException("Python bridge script not found (" + scriptPath + "). Set"
          + " PROJECT_ROOT (or the project.root system property) to the repository root.",
          new FileNotFoundException(String.valueOf(scriptPath)));
    }
    return scriptPath;
  }

  /**
   * Searches the working directory and its ancestors for the bridge script.
   *
   * @return the path to the script, or {@code null} if no ancestor contains it.
   */
  private static Path discoverScriptPath() {
    Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (dir != null) {
      Path candidate = dir.resolve(BRIDGE_SCRIPT);
      if (Files.exists(candidate)) {
        return candidate;
      }
      dir = dir.getParent();
    }
    return null;
  }

  /**
   * Builds a JSON request for the subprocess to get the user's badges.
   *
   * @param userId the user's ID
   * @return the JSON request envelope.
   */
  public static String buildGetUserBadges(UUID userId) {
    Request<UserIdPayload> request = new Request<>(
        RequestType.GET_USER_BADGES,
        new UserIdPayload(userId)
    );
    return GSON.toJson(request);
  }

  /**
   * Builds a JSON request for the subprocess to get the user's friends.
   *
   * @param userId the user's ID
   * @return the JSON request envelope.
   */
  public static String buildGetUserFriends(UUID userId) {
    Request<UserIdPayload> request = new Request<>(
        RequestType.GET_USER_FRIENDS,
        new UserIdPayload(userId)
    );

    return GSON.toJson(request);
  }

  /**
   * Builds a JSON request for the subprocess to award a badge to a user.
   *
   * @param userId the user's ID
   * @param badgeName the name of the badge to award
   * @return the JSON request envelope.
   */
  public static String buildAwardBadge(UUID userId, String badgeName) {
    Request<AwardBadgePayload> request = new Request<>(
        RequestType.AWARD_BADGE,
        new AwardBadgePayload(userId, badgeName)
    );

    return GSON.toJson(request);
  }

  /**
   * Builds a JSON request for the subprocess to get a user's event recommendations.
   *
   * @param userId the user's ID
   * @return the JSON request envelope.
   */
  public static String buildGetRecommendedEvents(UUID userId) {
    Request<UserIdPayload> request = new Request<>(
        RequestType.GET_RECOMMENDED_EVENTS,
        new UserIdPayload(userId)
    );

    return GSON.toJson(request);
  }

  /**
   * Builds a JSON request for the subprocess to record a user's attendance to an event.
   *
   * @param userId the user's ID
   * @param eventId the event's ID
   * @return the JSON request envelope.
   */
  public static String buildRecordAttendance(UUID userId, UUID eventId) {
    Request<AttendancePayload> request = new Request<>(
        RequestType.RECORD_ATTENDANCE,
        new AttendancePayload(userId, eventId)
    );

    return GSON.toJson(request);
  }

  /**
   * Builds a JSON request for the subprocess to get a user's friend recommendations.
   *
   * @param userId the user's ID
   * @return the JSON request envelope.
   */
  public static String buildGetRecommendedFriends(UUID userId) {
    Request<UserIdPayload> request = new Request<>(
        RequestType.GET_RECOMMENDED_FRIENDS,
        new UserIdPayload(userId)
    );

    return GSON.toJson(request);
  }


  /**
   * Sends a JSON request to the subprocess and returns its JSON response.
   *
   * @param requestJson the JSON request to send to the subprocess; must be a
   *     valid JSON string.
   *
   * @return the JSON response from the subprocess.
   *
   * @throws RuntimeException if the subprocess fails to process the request.
   */
  public static String sendRequest(String requestJson) {

    String response;
    int exitCode;

    try {

      // Launch the Python bridge as a fresh subprocess.
      Path scriptPath = resolveScriptPath();
      ProcessBuilder processBuilder = new ProcessBuilder(
          "python",
          scriptPath.toString()
      );
      Process process = processBuilder.start();

      // Write the request to the subprocess's standard input, then close it.
      try (OutputStream os = process.getOutputStream()) {

        OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(osw);

        writer.write(requestJson);
        writer.newLine();
        writer.flush();

      }

      // Read the response back from the subprocess's standard output.
      StringBuilder responseBuilder = new StringBuilder();
      try (
          InputStream is = process.getInputStream();
          InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
          BufferedReader reader = new BufferedReader(isr)
      ) {
        String line;
        while ((line = reader.readLine()) != null) {
          responseBuilder.append(line);
        }
      }

      response = responseBuilder.toString();
      exitCode = process.waitFor();

    } catch (Exception e) {
      throw new RuntimeException(String.format("Failed to process JSON: %s", requestJson), e);
    }

    // A non-zero exit code means the subprocess returned an error envelope; surface its message.
    if (exitCode != 0) {
      throw new RuntimeException(
          "Subprocess failed (exit code " + exitCode + "): " + extractError(response));
    }

    return response;
  }

  /**
   * Extracts the {@code error} message from a response envelope.
   *
   * @param response the JSON response envelope from the subprocess.
   * @return the value of the envelope's {@code error} field, or the raw response if it cannot be
   *     parsed.
   */
  private static String extractError(String response) {
    try {
      JsonObject envelope = JsonParser.parseString(response).getAsJsonObject();
      if (envelope.has("error") && !envelope.get("error").isJsonNull()) {
        return envelope.get("error").getAsString();
      }
    } catch (RuntimeException e) {
      // Fall through to returning the raw response below.
    }
    return response;
  }
  
}