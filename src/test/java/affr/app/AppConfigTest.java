package affr.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AppConfig#parse(java.util.List)}.
 *
 * <p>{@code AppConfig} is a pure record with no JavaFX dependency, so these tests run on the JUnit
 * platform alone.
 */
final class AppConfigTest {

  @Test
  void defaultsApplyWhenNoArgsGiven() {
    AppConfig cfg = AppConfig.parse(List.of());

    assertEquals("release", cfg.profile());
    assertNull(cfg.tutorialDir());
    assertFalse(cfg.isDebug());
  }

  @Test
  void profileEqualsForm() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=debug"));

    assertEquals("debug", cfg.profile());
    assertTrue(cfg.isDebug());
  }

  @Test
  void profileSpaceSeparatedForm() {
    AppConfig cfg = AppConfig.parse(List.of("--profile", "staging"));

    assertEquals("staging", cfg.profile());
    assertFalse(cfg.isDebug());
  }

  @Test
  void tutorialDirEqualsForm() {
    AppConfig cfg = AppConfig.parse(List.of("--tutorial-dir=/tmp/tutorials"));

    assertEquals(Path.of("/tmp/tutorials"), cfg.tutorialDir());
  }

  @Test
  void tutorialDirSpaceSeparatedForm() {
    AppConfig cfg = AppConfig.parse(List.of("--tutorial-dir", "/var/data/tutorials"));

    assertEquals(Path.of("/var/data/tutorials"), cfg.tutorialDir());
  }

  @Test
  void unrecognisedArgumentsAreSilentlyIgnored() {
    AppConfig cfg = AppConfig.parse(List.of("--unknown", "value", "--profile=debug"));

    assertEquals("debug", cfg.profile());
    assertNull(cfg.tutorialDir());
  }

  @Test
  void trailingFlagWithoutValueIsIgnored() {
    // "--profile" as the last token has no value — must not throw, defaults retained.
    AppConfig cfg = AppConfig.parse(List.of("--profile"));

    assertEquals("release", cfg.profile());
  }

  @Test
  void multipleFlagsCombine() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=debug", "--tutorial-dir", "/opt/tut"));

    assertEquals("debug", cfg.profile());
    assertEquals(Path.of("/opt/tut"), cfg.tutorialDir());
    assertTrue(cfg.isDebug());
  }

  @Test
  void laterProfileFlagOverridesEarlier() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=debug", "--profile=release"));

    assertEquals("release", cfg.profile());
    assertFalse(cfg.isDebug());
  }
}
