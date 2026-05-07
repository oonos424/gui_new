package affr.app;

import java.nio.file.Path;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable configuration derived from the application's command-line arguments.
 *
 * <p>Recognised options:
 *
 * <ul>
 *   <li>{@code --profile=<name>} — active profile (default: {@code "release"})
 *   <li>{@code --tutorial-dir <path>} — directory containing bundled tutorial projects
 * </ul>
 */
public record AppConfig(Profile profile, @Nullable Path tutorialDir) {

  /** The set of recognised build profiles. */
  public enum Profile {
    RELEASE("release"),
    DEBUG("debug");

    private final String key;

    Profile(String key) {
      this.key = key;
    }

    /** Returns the command-line name for this profile (e.g. {@code "release"}). */
    public String key() {
      return key;
    }

    /**
     * Returns the {@code Profile} whose key matches {@code value} (case-insensitive), or {@code
     * RELEASE} if no match is found.
     */
    public static Profile fromKey(String value) {
      for (Profile p : values()) {
        if (p.key.equalsIgnoreCase(value)) {
          return p;
        }
      }
      return RELEASE;
    }
  }

  /**
   * Parses {@code rawArgs} (as returned by {@link
   * javafx.application.Application.Parameters#getRaw()}) into an {@code AppConfig}.
   *
   * <p>Unrecognised arguments are silently ignored.
   */
  public static AppConfig parse(List<String> rawArgs) {
    Profile profile = Profile.RELEASE;
    @Nullable Path tutorialDir = null;

    int i = 0;
    while (i < rawArgs.size()) {
      String arg = rawArgs.get(i);
      if (arg.startsWith("--profile=")) {
        profile = Profile.fromKey(arg.substring("--profile=".length()));
      } else if (arg.equals("--profile") && i + 1 < rawArgs.size()) {
        profile = Profile.fromKey(rawArgs.get(++i));
      } else if (arg.startsWith("--tutorial-dir=")) {
        tutorialDir = Path.of(arg.substring("--tutorial-dir=".length()));
      } else if (arg.equals("--tutorial-dir") && i + 1 < rawArgs.size()) {
        tutorialDir = Path.of(rawArgs.get(++i));
      }
      i++;
    }

    return new AppConfig(profile, tutorialDir);
  }

  /** Returns {@code true} if the active profile is {@link Profile#DEBUG}. */
  public boolean isDebug() {
    return profile == Profile.DEBUG;
  }
}
