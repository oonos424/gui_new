package affr.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.app.AppConfig.Profile;
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

    assertEquals(Profile.RELEASE, cfg.profile());
    assertFalse(cfg.isDebug());
  }

  @Test
  void profileEqualsForm() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=debug"));

    assertEquals(Profile.DEBUG, cfg.profile());
    assertTrue(cfg.isDebug());
  }

  @Test
  void profileSpaceSeparatedForm() {
    AppConfig cfg = AppConfig.parse(List.of("--profile", "debug"));

    assertEquals(Profile.DEBUG, cfg.profile());
    assertTrue(cfg.isDebug());
  }

  @Test
  void unknownProfileFallsBackToRelease() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=staging"));

    assertEquals(Profile.RELEASE, cfg.profile());
    assertFalse(cfg.isDebug());
  }

  @Test
  void profileKeyIsCaseInsensitive() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=DEBUG"));

    assertEquals(Profile.DEBUG, cfg.profile());
    assertTrue(cfg.isDebug());
  }

  @Test
  void unrecognisedArgumentsAreSilentlyIgnored() {
    AppConfig cfg = AppConfig.parse(List.of("--unknown", "value", "--profile=debug"));

    assertEquals(Profile.DEBUG, cfg.profile());
  }

  @Test
  void trailingFlagWithoutValueIsIgnored() {
    // "--profile" as the last token has no value — must not throw, defaults retained.
    AppConfig cfg = AppConfig.parse(List.of("--profile"));

    assertEquals(Profile.RELEASE, cfg.profile());
  }

  @Test
  void multipleFlagsCombine() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=debug", "--unknown-flag", "foo"));

    assertEquals(Profile.DEBUG, cfg.profile());
    assertTrue(cfg.isDebug());
  }

  @Test
  void laterProfileFlagOverridesEarlier() {
    AppConfig cfg = AppConfig.parse(List.of("--profile=debug", "--profile=release"));

    assertEquals(Profile.RELEASE, cfg.profile());
    assertFalse(cfg.isDebug());
  }
}
