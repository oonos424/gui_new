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
public record AppConfig(String profile, @Nullable Path tutorialDir) {

  /**
   * Parses {@code rawArgs} (as returned by {@link
   * javafx.application.Application.Parameters#getRaw()}) into an {@code AppConfig}.
   *
   * <p>Unrecognised arguments are silently ignored.
   */
  public static AppConfig parse(List<String> rawArgs) {
    String profile = "release";
    @Nullable Path tutorialDir = null;

    int i = 0;
    while (i < rawArgs.size()) {
      String arg = rawArgs.get(i);
      if (arg.startsWith("--profile=")) {
        profile = arg.substring("--profile=".length());
      } else if (arg.equals("--profile") && i + 1 < rawArgs.size()) {
        profile = rawArgs.get(++i);
      } else if (arg.startsWith("--tutorial-dir=")) {
        tutorialDir = Path.of(arg.substring("--tutorial-dir=".length()));
      } else if (arg.equals("--tutorial-dir") && i + 1 < rawArgs.size()) {
        tutorialDir = Path.of(rawArgs.get(++i));
      }
      i++;
    }

    return new AppConfig(profile, tutorialDir);
  }

  /** Returns {@code true} if the active profile is {@code "debug"}. */
  public boolean isDebug() {
    return "debug".equals(profile);
  }
}
