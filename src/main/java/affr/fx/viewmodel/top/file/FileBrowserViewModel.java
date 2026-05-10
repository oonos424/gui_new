package affr.fx.viewmodel.top.file;

import affr.data.BrowserEntry;
import affr.data.DataStore;
import affr.data.ProjectEntry;
import affr.project.AFFrProject;
import affr.project.ProjectLoader;
import affr.util.fx.FxScheduler;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ViewModel for the File-browser view (FILE category).
 *
 * <p>Owns the observable state of a single-directory browser <em>and</em> the asynchronous IO
 * operations that drive that state: directory navigation, project creation, and project loading.
 * Background work is dispatched through an injected {@link FxScheduler} so the View never has to
 * spin up its own threads, and so this class can be unit-tested with {@link
 * FxScheduler#synchronous()}.
 *
 * <p>Failures from any of those operations are surfaced through {@link #errorProperty()} as the
 * most-recent {@link Throwable}; controllers observe and present (e.g. via an alert) and call
 * {@link #clearError()} to acknowledge.
 *
 * <p>This class contains no scene-graph imports.
 *
 * <p>All mutating methods must be called on the JavaFX Application Thread.
 */
public final class FileBrowserViewModel {

  private final DataStore dataStore;
  private final ProjectLoader projectLoader;
  private final FxScheduler scheduler;

  private final ObjectProperty<Path> currentPath;
  private final ObservableList<BrowserEntry> items = FXCollections.observableArrayList();
  private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
  private final ObjectProperty<@Nullable BrowserEntry> selectedItem =
      new SimpleObjectProperty<>(null);

  // Set by the VM once a directory reload completes after createProject so the controller can
  // focus the just-created entry. The controller observes and clears it.
  private final ReadOnlyObjectWrapper<@Nullable String> pendingSelectName =
      new ReadOnlyObjectWrapper<>(null);

  // Set by the controller when the user requests to open a project (double-click). The VM itself
  // observes this and starts the load via openProject().
  private final ObjectProperty<@Nullable ProjectEntry> openingProject =
      new SimpleObjectProperty<>(null);

  // Set by the VM when an open-project load succeeds. NavigationService observes and transitions
  // to the project screen.
  private final ReadOnlyObjectWrapper<@Nullable AFFrProject> openedProject =
      new ReadOnlyObjectWrapper<>(null);

  // Most-recent failure from any async operation, or null if the last operation succeeded or no
  // operation has run yet. Controllers display and call clearError() to acknowledge.
  private final ReadOnlyObjectWrapper<@Nullable Throwable> error =
      new ReadOnlyObjectWrapper<>(null);

  // Presentation-only: which layout mode is active (LIST, ICON, …)
  private final ObjectProperty<FileBrowserViewMode> viewMode =
      new SimpleObjectProperty<>(FileBrowserViewMode.LIST);

  /**
   * Creates a ViewModel backed by {@code dataStore} and {@code projectLoader}, dispatching async
   * work via {@code scheduler}. The browser starts positioned at the workspace root; trigger the
   * initial load by calling {@link #navigateTo(Path)} with {@code dataStore.getRootPath()}.
   */
  public FileBrowserViewModel(
      DataStore dataStore, ProjectLoader projectLoader, FxScheduler scheduler) {
    this.dataStore = dataStore;
    this.projectLoader = projectLoader;
    this.scheduler = scheduler;
    this.currentPath = new SimpleObjectProperty<>(dataStore.getRootPath());

    // Wire opening-project trigger: as soon as the controller sets openingProject, start the load.
    openingProject.addListener(
        (obs, old, entry) -> {
          if (entry != null) {
            openProject(entry);
          }
        });
  }

  // ── Current path ──────────────────────────────────────────────────────────

  public ObjectProperty<Path> currentPathProperty() {
    return currentPath;
  }

  public Path getCurrentPath() {
    return currentPath.get();
  }

  /**
   * Direct setter, retained for callers that need to update the path without triggering a reload
   * (e.g. tree-mode selection that changes the "current folder" used by New Project). Prefer {@link
   * #navigateTo(Path)} for normal navigation.
   */
  public void setCurrentPath(Path path) {
    currentPath.set(path);
  }

  // ── Items ─────────────────────────────────────────────────────────────────

  /** The items visible in the browser for the current path. */
  public ObservableList<BrowserEntry> getItems() {
    return items;
  }

  /** Replaces the item list atomically. Must be called on the JavaFX Application Thread. */
  public void setItems(List<BrowserEntry> newItems) {
    items.setAll(newItems);
  }

  // ── Loading state ─────────────────────────────────────────────────────────

  public ReadOnlyBooleanProperty loadingProperty() {
    return loading.getReadOnlyProperty();
  }

  public boolean isLoading() {
    return loading.get();
  }

  public void setLoading(boolean value) {
    loading.set(value);
  }

  // ── Selection ─────────────────────────────────────────────────────────────

  public ObjectProperty<@Nullable BrowserEntry> selectedItemProperty() {
    return selectedItem;
  }

  public @Nullable BrowserEntry getSelectedItem() {
    return selectedItem.get();
  }

  public void setSelectedItem(@Nullable BrowserEntry item) {
    selectedItem.set(item);
  }

  // ── Opening / opened project ──────────────────────────────────────────────

  /**
   * Trigger property: the controller sets this to a {@link ProjectEntry} when the user
   * double-clicks; the VM observes and starts the async load. Reset to {@code null} after the
   * project transition so the same project can be re-opened.
   */
  public ObjectProperty<@Nullable ProjectEntry> openingProjectProperty() {
    return openingProject;
  }

  public @Nullable ProjectEntry getOpeningProject() {
    return openingProject.get();
  }

  public void setOpeningProject(@Nullable ProjectEntry entry) {
    openingProject.set(entry);
  }

  /**
   * Result property: set by the VM to the loaded {@link AFFrProject} once an open-project load
   * succeeds. {@code NavigationService} observes and transitions to the project screen, then clears
   * the value back to {@code null} so the same project can be reopened.
   */
  public ReadOnlyObjectProperty<@Nullable AFFrProject> openedProjectProperty() {
    return openedProject.getReadOnlyProperty();
  }

  public @Nullable AFFrProject getOpenedProject() {
    return openedProject.get();
  }

  /** Clears the {@code openedProject} so the same project may be opened again. */
  public void clearOpenedProject() {
    openedProject.set(null);
  }

  // ── Error channel ─────────────────────────────────────────────────────────

  /**
   * The most-recent failure from any async operation, or {@code null} if the last operation
   * succeeded or no operation has run yet. Listeners present and call {@link #clearError()} to
   * acknowledge.
   */
  public ReadOnlyObjectProperty<@Nullable Throwable> errorProperty() {
    return error.getReadOnlyProperty();
  }

  public @Nullable Throwable getError() {
    return error.get();
  }

  /** Acknowledges the current error so future failures will fire a fresh listener notification. */
  public void clearError() {
    error.set(null);
  }

  // ── Pending selection (post-create reload) ────────────────────────────────

  /**
   * The name of an entry the controller should focus after the next reload completes, or {@code
   * null} if there is no pending selection. Set by {@link #createProject(Path, String, String)}
   * once the new directory is created; the controller observes, applies the selection, and calls
   * {@link #clearPendingSelectName()}.
   */
  public ReadOnlyObjectProperty<@Nullable String> pendingSelectNameProperty() {
    return pendingSelectName.getReadOnlyProperty();
  }

  public @Nullable String getPendingSelectName() {
    return pendingSelectName.get();
  }

  public void clearPendingSelectName() {
    pendingSelectName.set(null);
  }

  // ── View mode ─────────────────────────────────────────────────────────────

  /**
   * Observable display mode (LIST, ICON, …). The controller observes this property and swaps the
   * visible content node accordingly.
   */
  public ObjectProperty<FileBrowserViewMode> viewModeProperty() {
    return viewMode;
  }

  public FileBrowserViewMode getViewMode() {
    return viewMode.get();
  }

  public void setViewMode(FileBrowserViewMode mode) {
    viewMode.set(mode);
  }

  // ── Navigation helpers ────────────────────────────────────────────────────

  /** {@code true} when the browser is already at the workspace root. */
  public boolean isAtRoot() {
    return currentPath.get().equals(dataStore.getRootPath());
  }

  /**
   * Returns the logical parent of the current path, clamped to the workspace root so the browser
   * can never navigate above it.
   */
  public Path parentPath() {
    Path current = currentPath.get();
    Path root = dataStore.getRootPath();
    if (current.equals(root)) {
      return root;
    }
    Path parent = current.getParent();
    if (parent == null || !parent.startsWith(root)) {
      return root;
    }
    return parent;
  }

  /** Lists the children of the workspace root. Convenience wrapper around the data store. */
  public Path getRootPath() {
    return dataStore.getRootPath();
  }

  // ── Async operations ──────────────────────────────────────────────────────

  /**
   * Navigates the browser to {@code path} and reloads its children asynchronously. Sets {@link
   * #loadingProperty()} to {@code true} while the load is in flight; on success replaces {@link
   * #getItems()} with the loaded entries; on failure sets {@link #errorProperty()} and leaves items
   * empty.
   */
  public void navigateTo(Path path) {
    navigateTo(path, null);
  }

  /**
   * Like {@link #navigateTo(Path)} but, on success, additionally sets {@link
   * #pendingSelectNameProperty()} to {@code selectAfter} so the controller can focus the matching
   * entry. The pending-select fires <em>after</em> items have been published, so listeners can read
   * the new entries when the property fires.
   */
  private void navigateTo(Path path, @Nullable String selectAfter) {
    setLoading(true);
    setCurrentPath(path);
    items.clear();
    runIo(
        () -> {
          try {
            List<BrowserEntry> loaded = dataStore.loadChildren(path);
            scheduler.runUi(
                () -> {
                  setItems(loaded);
                  setLoading(false);
                  if (selectAfter != null) {
                    pendingSelectName.set(selectAfter);
                  }
                });
          } catch (IOException | RuntimeException e) {
            scheduler.runUi(
                () -> {
                  items.clear();
                  setLoading(false);
                  error.set(e);
                });
          }
        });
  }

  /**
   * Navigates to the parent directory if not already at the workspace root. No-op at root.
   * Equivalent to {@code navigateTo(parentPath())} when {@link #isAtRoot()} is false.
   */
  public void navigateUp() {
    if (!isAtRoot()) {
      navigateTo(parentPath());
    }
  }

  /**
   * Creates a new project named {@code name} (with optional {@code memo}) inside {@code parentDir},
   * then reloads {@code parentDir} so the new project becomes visible. After the reload completes
   * {@link #pendingSelectNameProperty()} fires with the new entry's name so the controller can
   * focus it.
   *
   * <p>On failure, sets {@link #errorProperty()} and leaves the browser unchanged.
   */
  public void createProject(Path parentDir, String name, String memo) {
    runIo(
        () -> {
          try {
            ProjectEntry created = dataStore.createProject(parentDir, name, memo);
            scheduler.runUi(() -> navigateTo(parentDir, created.name()));
          } catch (IOException | RuntimeException e) {
            scheduler.runUi(() -> error.set(e));
          }
        });
  }

  /**
   * Loads the children of {@code path} asynchronously and delivers the result via {@code callback}
   * on the FX thread. On success, {@code callback.accept(children, null)} is called; on failure,
   * {@code callback.accept(emptyList, throwable)}. Used by tree-mode lazy expansion which has its
   * own per-node loading semantics and does not touch the main {@link #getItems()} list.
   */
  public void loadChildrenAsync(
      Path path, BiConsumer<List<BrowserEntry>, @Nullable Throwable> callback) {
    runIo(
        () -> {
          try {
            List<BrowserEntry> loaded = dataStore.loadChildren(path);
            scheduler.runUi(() -> callback.accept(loaded, null));
          } catch (IOException | RuntimeException e) {
            scheduler.runUi(() -> callback.accept(List.of(), e));
          }
        });
  }

  /**
   * Loads the project at {@code entry.path()} asynchronously and publishes the result via {@link
   * #openedProjectProperty()}. On failure sets {@link #errorProperty()} and clears {@link
   * #openingProjectProperty()} so the user can retry.
   */
  public void openProject(ProjectEntry entry) {
    Path projectPath = entry.path();
    runIo(
        () -> {
          try {
            AFFrProject project = projectLoader.load(projectPath);
            scheduler.runUi(() -> openedProject.set(project));
          } catch (IOException | RuntimeException e) {
            scheduler.runUi(
                () -> {
                  error.set(e);
                  openingProject.set(null);
                });
          }
        });
  }

  // ── Private ──────────────────────────────────────────────────────────────

  private void runIo(Runnable task) {
    scheduler.runIo(task);
  }
}
