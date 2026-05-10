package affr.app;

import affr.data.DataStore;
import affr.fx.viewmodel.top.file.FileBrowserViewMode;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.stage.Stage;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wires {@link UserPreferences} to the application's stateful surfaces in one place.
 *
 * <p>Centralises three persistence concerns that previously lived scattered across {@link
 * NavigationService} and the top-level controller:
 *
 * <ul>
 *   <li>Window size and position — saved on {@link Stage#setOnHiding(javafx.event.EventHandler)}.
 *   <li>Browser view mode and current path — saved when the corresponding {@link
 *       FileBrowserViewModel} properties change.
 *   <li>UI locale — saved when {@link I18n#bundleProperty()} changes.
 * </ul>
 *
 * <p>This class lives in {@code affr.app} (not {@code affr.util.prefs}) because it depends on
 * scene-graph types ({@link Stage}) and the ViewModel layer ({@link FileBrowserViewModel}), both of
 * which {@code affr.util} is forbidden from importing per the project's source-layout rules.
 */
public final class PreferencesAdapter {

  private final UserPreferences prefs;
  private final Stage stage;
  private final FileBrowserViewModel fileBrowserViewModel;

  public PreferencesAdapter(
      UserPreferences prefs, Stage stage, FileBrowserViewModel fileBrowserViewModel) {
    this.prefs = prefs;
    this.stage = stage;
    this.fileBrowserViewModel = fileBrowserViewModel;
  }

  // ── Restore ───────────────────────────────────────────────────────────────

  /**
   * Applies the saved browser view mode and directory path from {@link UserPreferences} to {@code
   * vm} before the controller is initialised. Falls back silently to defaults if the saved values
   * are absent, invalid, or point to a directory that no longer exists inside the workspace.
   *
   * @param vm the file-browser ViewModel to restore state into
   * @param dataStore the master data store, used to validate that the saved path still lives inside
   *     the workspace root
   */
  public void restoreInto(FileBrowserViewModel vm, DataStore dataStore) {
    @Nullable String savedMode = prefs.browserViewMode();
    if (savedMode != null) {
      try {
        vm.setViewMode(FileBrowserViewMode.valueOf(savedMode));
      } catch (IllegalArgumentException ignored) {
        // Unrecognised name — keep the default LIST mode.
      }
    }

    @Nullable Path savedPath = prefs.browserPath();
    if (savedPath != null) {
      try {
        Path rootPath = dataStore.getRootPath();
        if (savedPath.startsWith(rootPath) && Files.isDirectory(savedPath)) {
          vm.setCurrentPath(savedPath);
        }
      } catch (Exception ignored) {
        // IO error — keep the default root path.
      }
    }
  }

  // ── Install (persist listeners) ───────────────────────────────────────────

  /**
   * Installs all save listeners. Idempotent only insofar as each listener is added once per adapter
   * instance; do not call twice on the same instance.
   */
  public void install() {
    installWindowBoundsPersistence();
    installBrowserStatePersistence();
    installLocalePersistence();
  }

  /**
   * Installs the {@link Stage#setOnHiding(javafx.event.EventHandler)} hook that saves the window
   * size and position when the user closes the window. Exposed separately from {@link #install()}
   * so unit tests can install only the headless-friendly branches.
   */
  public void installWindowBoundsPersistence() {
    stage.setOnHiding(
        e -> {
          double w = stage.getWidth();
          double h = stage.getHeight();
          if (Double.isFinite(w) && w > 0 && Double.isFinite(h) && h > 0) {
            prefs.setWindowSize(w, h);
            prefs.setWindowPosition(stage.getX(), stage.getY());
            prefs.save();
          }
        });
  }

  /**
   * Installs listeners that persist the file-browser view mode and current directory whenever they
   * change. Exposed separately from {@link #install()} for unit testing.
   */
  public void installBrowserStatePersistence() {
    fileBrowserViewModel
        .viewModeProperty()
        .addListener(
            (obs, old, mode) -> {
              prefs.setBrowserViewMode(mode.name());
              prefs.save();
            });

    fileBrowserViewModel
        .currentPathProperty()
        .addListener(
            (obs, old, path) -> {
              prefs.setBrowserPath(path);
              prefs.save();
            });
  }

  /**
   * Installs a listener that persists the active UI locale whenever {@link I18n#bundleProperty()}
   * changes. Exposed separately from {@link #install()} for unit testing.
   */
  public void installLocalePersistence() {
    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              prefs.setLocale(I18n.getLocale());
              prefs.save();
            });
  }
}
