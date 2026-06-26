package york.studentevents.subprocess;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.UncheckedIOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SubprocessRequestFactory}'s bridge-script path resolution.
 *
 * <p>These manipulate the {@code project.root} and {@code user.dir} system properties to drive the
 * explicit-configuration and walk-up discovery branches of {@code resolveScriptPath}, restoring
 * them afterwards. They are skipped when {@code PROJECT_ROOT} is set, since it would override the
 * property under test.
 */
class SubprocessRequestFactoryPathTest {

  private static final UUID USER_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private String previousProjectRoot;
  private String previousUserDir;

  @BeforeEach
  void capture() {
    assumeTrue(System.getenv("PROJECT_ROOT") == null, "PROJECT_ROOT env overrides these tests");
    previousProjectRoot = System.getProperty("project.root");
    previousUserDir = System.getProperty("user.dir");
  }

  @AfterEach
  void restore() {
    restoreProperty("project.root", previousProjectRoot);
    restoreProperty("user.dir", previousUserDir);
  }

  private static void restoreProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  private static boolean python3Available() {
    try {
      Process process = new ProcessBuilder("python3", "--version").start();
      return process.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  @Test
  void configuredRootWithoutScriptFails() {
    System.setProperty("project.root", System.getProperty("java.io.tmpdir"));
    RuntimeException exception = assertThrows(
        RuntimeException.class, () -> SubprocessRequestFactory.sendRequest("{}"));
    assertInstanceOf(UncheckedIOException.class, exception.getCause());
  }

  @Test
  void discoveryFailsWhenNoAncestorHasScript() {
    System.clearProperty("project.root");
    System.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
    RuntimeException exception = assertThrows(
        RuntimeException.class, () -> SubprocessRequestFactory.sendRequest("{}"));
    assertInstanceOf(UncheckedIOException.class, exception.getCause());
  }

  @Test
  void discoveryFindsScriptWhenUnconfigured() {
    assumeTrue(python3Available(), "python3 is not available on PATH");
    System.clearProperty("project.root");
    // user.dir is the module directory under surefire; discovery walks up to the repo root.
    String response = SubprocessRequestFactory.sendRequest(
        SubprocessRequestFactory.buildGetUserBadges(USER_ID));
    assertTrue(response.contains("\"status\""));
  }
}
