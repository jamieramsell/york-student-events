package york.studentevents.subprocess;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

/** Factory class for building requests to the subprocess. */
class SubprocessRequestFactory {

  /**
   * Request class for the subprocess communication.
   *
   * @param T the type of the payload
   * @param requestType the type of the request
   * @param payload the payload of the request
   * 
   * @see UserIdPayload
   * @see AwardBadgePayload
   * @see RequestType
   */
  private static record Request<T>(RequestType requestType, T payload) {}

  /**
   * Builds a request for the subprocess where the payload is only a user ID.
   *
   * @param userId the user's ID
   */
  static record UserIdPayload(UUID userId) {}

  /**
   * Builds a request for the subprocess where the payload is a user ID and a badge name.
   *
   * @param userId the user's ID
   * @param badgeName the name of the badge to award
   */
  static record AwardBadgePayload(UUID userId, String badgeName) {}

  private static final Gson GSON = new Gson();
  Scanner in = new Scanner(System.in);

  /**
   * Builds a JSON request for the subprocess to get the user's badges.
   *
   * @param userId the user's ID
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
   */
  public static String buildAwardBadge(UUID userId, String badgeName) {
    Request<AwardBadgePayload> request = new Request<>(
        RequestType.AWARD_BADGE,
        new AwardBadgePayload(userId, badgeName)
    );

    return GSON.toJson(request);
  }

  /**
   * Takes a JSON request and sends it to the subprocess and returns the JSON
   * response.
   *
   * @param requestJson the JSON request to send to the subprocess; must be a
   *     valid JSON string.
   *
   * @return the JSON response from the subprocess.
   *
   * @throws RuntimeException if the subprocess fails to process the request.
   */
  public String processBuilder(String requestJson) {

    try {

      ProcessBuilder processBuilder = new ProcessBuilder(
          "python",
          "api-core/src/subprocess_bridge.py"
      );
      Process process = processBuilder.start();

      try (OutputStream os = process.getOutputStream()) {

        OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(osw);

        writer.write(requestJson);
        writer.newLine();
        writer.flush();

      }

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