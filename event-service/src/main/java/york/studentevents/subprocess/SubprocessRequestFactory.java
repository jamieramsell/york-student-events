package york.studentevents.subprocess;

import com.google.gson.Gson;
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
   * @see RequestType
   */
  static record Request<T extends IPayload>(RequestType requestType, T payload) {}

  private static final Gson GSON = new Gson();
  private static final String BRIDGE_SCRIPT = "api-core/src/bridge/responder.py";
  
  /**
   * Resolves the absolute path to the Python bridge script, anchored to the project root.
   *
   * <p>The project root is taken from the {@code PROJECT_ROOT} environment variable, falling back
   * to the {@code project.root} system property, and finally to the JVM working directory
   * ({@code user.dir}). Anchoring to an explicit root lets the script be found regardless of the
   * directory the JVM was launched from.
   *
   * @return the absolute, normalised path to {@code responder.py}.
   *
   * @throws UncheckedIOException if the script does not exist at the resolved location.
   */
  private static Path resolveScriptPath() {
    String root = System.getenv("PROJECT_ROOT");
    if (root == null || root.isBlank()) {
      root = System.getProperty("project.root");
    }
    if (root == null || root.isBlank()) {
      root = System.getProperty("user.dir");
    }

    Path scriptPath = Path.of(root, BRIDGE_SCRIPT).toAbsolutePath().normalize();
    if (!Files.exists(scriptPath)) {
      throw new UncheckedIOException("Python bridge script not found at: " + scriptPath + ". Set"
          + " PROJECT_ROOT (or the project.root system property) to the repository root.",
          new FileNotFoundException(scriptPath.toString()));
    }
    return scriptPath;
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

    try {

      // Launch the Python bridge as a fresh subprocess.
      Path scriptPath = resolveScriptPath();
      ProcessBuilder processBuilder = new ProcessBuilder(
          "python3",
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

      // A non-zero exit code means the subprocess failed to handle the request.
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("Python script failed with exit code: " + exitCode);
      }

      return responseBuilder.toString();

    } catch (Exception e) {
      throw new RuntimeException(String.format("Failed to process JSON: %s", requestJson), e);
    }

  }
  
}