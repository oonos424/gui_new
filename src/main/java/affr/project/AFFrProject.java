package affr.project;

import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Domain object for an AFFr project.
 *
 * <p>Holds the observable list of {@link ProjectItem}s and the transient focused-item selection.
 * Sort order is a presentation concern and lives in the ViewModel layer, not here.
 *
 * <p>The {@link ObservableList} and {@link ObjectProperty} allow the ViewModel and View layers to
 * bind directly without polling. Both may be accessed from any thread for reads and from the JavaFX
 * Application Thread for writes that propagate to bound UI nodes.
 */
public final class AFFrProject {

  private final String name;
  private final Path path;
  private final String memo;
  private final ObservableList<ProjectItem> items;

  // Transient: not persisted in this phase. Focus is restored only when .current_focus
  // persistence is implemented alongside the Input Editor.
  private final ObjectProperty<@Nullable ProjectItem> focusedItem =
      new SimpleObjectProperty<>(this, "focusedItem", null);

  /**
   * Creates a project with the given metadata and initial item list.
   *
   * @param name directory name (display name)
   * @param path absolute path to the project directory
   * @param memo free-text memo read from {@code .affr_project}; empty if not set
   * @param initialItems the items to populate the list with (copied into an observable list)
   */
  public AFFrProject(String name, Path path, String memo, List<ProjectItem> initialItems) {
    this.name = name;
    this.path = path;
    this.memo = memo;
    this.items = FXCollections.observableArrayList(initialItems);
  }

  /** The project's directory name (used as the display name). */
  public String getName() {
    return name;
  }

  /** Absolute path to the project directory on disk. */
  public Path getPath() {
    return path;
  }

  /** Free-text memo read from {@code .affr_project}; empty string if not set. */
  public String getMemo() {
    return memo;
  }

  /**
   * The live list of project items. Loaded in scan order; the ViewModel layer applies sorting via
   * {@link javafx.collections.transformation.SortedList}.
   */
  public ObservableList<ProjectItem> getItems() {
    return items;
  }

  /** Observable property for the currently focused item; {@code null} when nothing is selected. */
  public ObjectProperty<@Nullable ProjectItem> focusedItemProperty() {
    return focusedItem;
  }

  /** Returns the currently focused item, or {@code null} when nothing is selected. */
  public @Nullable ProjectItem getFocusedItem() {
    return focusedItem.get();
  }

  /** Sets the focused item. Pass {@code null} to clear the selection. */
  public void setFocusedItem(@Nullable ProjectItem item) {
    focusedItem.set(item);
  }
}
