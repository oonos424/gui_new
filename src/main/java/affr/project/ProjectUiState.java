package affr.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Per-project UI state, stored in {@code .affr_ui} inside the project directory.
 *
 * <p>This is the project-scoped counterpart of the global {@code UserPreferences}. It persists
 * lightweight, UI-only hints that are specific to one project — for example, which directory the
 * user last navigated to when picking a solver control file. The file is not user-facing and is
 * safe to delete; the application will recreate it with defaults on the next relevant action.
 *
 * <p>IO errors on load are silently ignored (defaults are used). IO errors on save are also
 * silently ignored — this state is best-effort, not critical.
 */
public final class ProjectUiState {

  static final String FILENAME = ".affr_ui";

  private static final String KEY_CTL_LAST_DIR = "ctl.lastDir";

  private final Path stateFile;
  private @Nullable Path ctlLastDir;

  private ProjectUiState(Path stateFile, @Nullable Path ctlLastDir) {
    this.stateFile = stateFile;
    this.ctlLastDir = ctlLastDir;
  }

  // ── Factory ────────────────────────────────────────────────────────────────

  /**
   * Loads the UI state for the project rooted at {@code projectPath}. Returns an instance with
   * default (null) values if the state file is absent or unreadable.
   */
  public static ProjectUiState load(Path projectPath) {
    Path file = projectPath.resolve(FILENAME);
    Properties props = new Properties();
    if (Files.exists(file)) {
      try (InputStream in = Files.newInputStream(file)) {
        props.load(in);
      } catch (IOException ignored) {
      }
    }
    return new ProjectUiState(file, parsePath(props.getProperty(KEY_CTL_LAST_DIR)));
  }

  // ── Accessors ──────────────────────────────────────────────────────────────

  /**
   * Returns the last directory the user navigated to when selecting a solver control file ({@code
   * *.ctl}) for this project, or {@code null} if none has been recorded yet.
   */
  public @Nullable Path getCtlLastDir() {
    return ctlLastDir;
  }

  /**
   * Records the directory from which the user last selected a solver control file. Call {@link
   * #save()} to persist the change.
   */
  public void setCtlLastDir(Path dir) {
    this.ctlLastDir = dir.toAbsolutePath();
  }

  // ── Persistence ────────────────────────────────────────────────────────────

  /**
   * Writes the current state to disk. IO failures are silently ignored — this state is best-effort.
   */
  public void save() {
    try {
      Properties props = new Properties();
      if (ctlLastDir != null) {
        props.setProperty(KEY_CTL_LAST_DIR, ctlLastDir.toAbsolutePath().toString());
      }
      try (OutputStream out = Files.newOutputStream(stateFile)) {
        props.store(out, "AFFr project UI state — safe to delete");
      }
    } catch (IOException ignored) {
    }
  }

  /**
   * Parses a persisted path value. Returns {@code null} if the value is absent, blank, or not a
   * syntactically valid path on this filesystem.
   */
  private static @Nullable Path parsePath(@Nullable String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Path.of(value);
    } catch (InvalidPathException e) {
      return null;
    }
  }
}
