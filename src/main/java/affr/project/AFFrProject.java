package affr.project;

import java.nio.file.Path;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Domain object for an AFFr project.
 *
 * <p>Holds the observable list of {@link ProjectItem}s. Sort order and the focused-item selection
 * are presentation concerns and live in the ViewModel layer, not here.
 *
 * <p>The {@link ObservableList} allows the ViewModel and View layers to bind directly without
 * polling. It may be accessed from any thread for reads and from the JavaFX Application Thread for
 * writes that propagate to bound UI nodes.
 */
public final class AFFrProject {

  private final String name;
  private final Path path;
  private final String memo;
  private final ObservableList<ProjectItem> items;

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
}
