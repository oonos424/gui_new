package affr.fx.viewmodel.top.file;

import affr.data.BrowserEntry;
import affr.data.DataStore;
import affr.data.ProjectEntry;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ViewModel for the File-browser view (FILE category).
 *
 * <p>Holds the observable state of a single-directory browser: which directory is currently
 * displayed, the {@link BrowserEntry} items it contains, whether a background load is in progress,
 * and which item the user has selected.
 *
 * <p>This class contains <em>no IO</em> and <em>no scene-graph imports</em>. Navigation and
 * background-thread dispatch live in {@code affr.app.top.file.FileBrowserController}.
 *
 * <p>All mutating methods must be called on the JavaFX Application Thread.
 */
public final class FileBrowserViewModel {

  private final DataStore dataStore;

  private final ObjectProperty<Path> currentPath;
  private final ObservableList<BrowserEntry> items = FXCollections.observableArrayList();
  private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper(false);
  private final ObjectProperty<@Nullable BrowserEntry> selectedItem =
      new SimpleObjectProperty<>(null);

  // Set by the controller when the user double-clicks a ProjectEntry; observed by TopController
  // which then loads the project and transitions the view.
  private final ObjectProperty<@Nullable ProjectEntry> openingProject =
      new SimpleObjectProperty<>(null);

  /**
   * Creates a ViewModel backed by {@code dataStore}. The browser starts positioned at the workspace
   * root; the initial load is triggered by the controller calling {@code
   * navigateTo(getRootPath())}.
   */
  public FileBrowserViewModel(DataStore dataStore) {
    this.dataStore = dataStore;
    this.currentPath = new SimpleObjectProperty<>(dataStore.getRootPath());
  }

  // ── DataStore access (read-only from outside) ────────────────────────────

  /** The backing data store — used by the controller to schedule IO. */
  public DataStore getDataStore() {
    return dataStore;
  }

  // ── Current path ──────────────────────────────────────────────────────────

  public ObjectProperty<Path> currentPathProperty() {
    return currentPath;
  }

  public Path getCurrentPath() {
    return currentPath.get();
  }

  /** Called by the controller at the start of a navigation before items are refreshed. */
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

  // ── Opening project ───────────────────────────────────────────────────────

  /**
   * Set by the controller when the user double-clicks a {@link ProjectEntry}. {@code TopController}
   * observes this property and loads the project in a background task. Reset to {@code null} after
   * the transition so the same project can be re-opened.
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
}
