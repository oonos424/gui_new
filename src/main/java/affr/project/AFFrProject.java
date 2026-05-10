package affr.project;

import java.nio.file.Path;
import java.util.List;

/**
 * Domain object for an AFFr project.
 *
 * <p>A pure data record of the project's identity and its initial item list. This class has no
 * JavaFX dependency — observable wrappers, sort order, and focused-item selection are all
 * presentation concerns that live in the ViewModel layer.
 */
public final class AFFrProject {

  private final String name;
  private final Path path;
  private final String memo;
  private final List<ProjectItem> items;

  /**
   * Creates a project with the given metadata and initial item list.
   *
   * @param name directory name (display name)
   * @param path absolute path to the project directory
   * @param memo free-text memo read from {@code .affr_project}; empty if not set
   * @param initialItems the items to populate the list with (copied defensively)
   */
  public AFFrProject(
      String name, Path path, String memo, List<? extends ProjectItem> initialItems) {
    this.name = name;
    this.path = path;
    this.memo = memo;
    this.items = List.copyOf(initialItems);
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
   * The project's item list as loaded from disk, in scan order. The ViewModel layer copies this
   * into its own {@link javafx.collections.ObservableList} and applies sorting there.
   */
  public List<ProjectItem> getItems() {
    return items;
  }
}
