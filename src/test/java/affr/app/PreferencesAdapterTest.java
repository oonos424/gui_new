package affr.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import affr.data.DataStore;
import affr.fx.viewmodel.top.file.FileBrowserViewMode;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.project.ProjectLoader;
import affr.util.fx.FxScheduler;
import affr.util.prefs.UserPreferences;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PreferencesAdapter}.
 *
 * <p>Uses a {@link TempDir} for both the {@code preferences.properties} file and the workspace
 * root, and {@link FxScheduler#synchronous()} so VM property changes propagate inline.
 *
 * <p>Stage-based persistence (window bounds) requires JavaFX initialisation and is exercised by the
 * higher-level integration tests; these unit tests focus on the browser-state and locale branches
 * of the adapter, which can run headless.
 */
final class PreferencesAdapterTest {

  // ── Restore tests ─────────────────────────────────────────────────────────

  @Test
  void restoreAppliesSavedViewMode(@TempDir Path tmp) throws IOException {
    Path prefsFile = tmp.resolve("prefs.properties");
    Files.writeString(prefsFile, "browser.viewMode=TREE\n");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);

    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);

    adapter.restoreInto(vm, ds);

    assertEquals(FileBrowserViewMode.TREE, vm.getViewMode());
  }

  @Test
  void restoreAppliesSavedPathWhenItIsInsideRoot(@TempDir Path tmp) throws IOException {
    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    Path savedDir = Files.createDirectory(workspace.resolve("sub"));

    Path prefsFile = tmp.resolve("prefs.properties");
    Files.writeString(prefsFile, "browser.path=" + savedDir + "\n");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);

    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);

    adapter.restoreInto(vm, ds);

    assertEquals(savedDir, vm.getCurrentPath());
  }

  @Test
  void restoreIgnoresPathOutsideWorkspaceRoot(@TempDir Path tmp) throws IOException {
    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    Path elsewhere = Files.createDirectory(tmp.resolve("elsewhere"));

    Path prefsFile = tmp.resolve("prefs.properties");
    Files.writeString(prefsFile, "browser.path=" + elsewhere + "\n");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);

    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);

    adapter.restoreInto(vm, ds);

    // Outside the workspace root → restore must be skipped.
    assertEquals(workspace, vm.getCurrentPath());
  }

  @Test
  void restoreIgnoresPathThatNoLongerExists(@TempDir Path tmp) throws IOException {
    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    Path missing = workspace.resolve("missing-subdir");

    Path prefsFile = tmp.resolve("prefs.properties");
    Files.writeString(prefsFile, "browser.path=" + missing + "\n");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);

    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);

    adapter.restoreInto(vm, ds);

    assertEquals(workspace, vm.getCurrentPath());
  }

  @Test
  void restoreIgnoresUnknownViewModeName(@TempDir Path tmp) throws IOException {
    Path prefsFile = tmp.resolve("prefs.properties");
    Files.writeString(prefsFile, "browser.viewMode=BOGUS\n");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);

    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);

    adapter.restoreInto(vm, ds);

    // Unknown name → keep the default LIST.
    assertEquals(FileBrowserViewMode.LIST, vm.getViewMode());
  }

  // ── Persist tests (browser-state branch) ──────────────────────────────────

  @Test
  void changingViewModePersistsToPrefs(@TempDir Path tmp) throws IOException {
    Path prefsFile = tmp.resolve("prefs.properties");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);
    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);
    adapter.installBrowserStatePersistence();

    vm.setViewMode(FileBrowserViewMode.ICON);

    assertEquals("ICON", prefs.browserViewMode());
    assertEquals("ICON", UserPreferences.loadFrom(prefsFile).browserViewMode());
  }

  @Test
  void changingCurrentPathPersistsToPrefs(@TempDir Path tmp) throws IOException {
    Path prefsFile = tmp.resolve("prefs.properties");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);
    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());
    PreferencesAdapter adapter = new PreferencesAdapter(prefs, dummyStage(), vm);
    adapter.installBrowserStatePersistence();

    Path sub = workspace.resolve("sub");
    vm.setCurrentPath(sub);

    assertEquals(sub, prefs.browserPath());
    assertEquals(sub, UserPreferences.loadFrom(prefsFile).browserPath());
  }

  @Test
  void noPersistenceFiresBeforeInstall(@TempDir Path tmp) throws IOException {
    Path prefsFile = tmp.resolve("prefs.properties");
    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);
    Path workspace = Files.createDirectory(tmp.resolve("workspace"));
    DataStore ds = new DataStore(workspace);
    FileBrowserViewModel vm =
        new FileBrowserViewModel(ds, new ProjectLoader(), FxScheduler.synchronous());

    // Construct adapter but DO NOT install any listener.
    new PreferencesAdapter(prefs, dummyStage(), vm);

    vm.setViewMode(FileBrowserViewMode.ICON);

    // Listener was never installed → prefs unchanged.
    assertNull(prefs.browserViewMode());
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  /**
   * Returns a Stage placeholder. The browser-state and locale tests never invoke {@link
   * javafx.stage.Stage#setOnHiding}, so a {@code null} reference would not be touched — but the
   * adapter constructor stores it, and we cannot construct a real Stage off the FX thread, so we
   * simply pass {@code null}. Tests that exercise window-bounds persistence belong in an
   * integration suite with a running FX toolkit.
   */
  @SuppressWarnings("nullness")
  private static javafx.stage.Stage dummyStage() {
    return null;
  }
}
