package affr.project;

import java.nio.file.Path;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

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
  private final boolean tutorial;

  /**
   * Writable mirror directory for user-created calculations in a tutorial project, or {@code null}
   * for regular projects. New calculations are written here rather than to the read-only tutorial
   * installation path. Existing calculations from both the tutorial installation dir and this
   * mirror are merged into {@link #items} at load time.
   */
  private final @Nullable Path mirrorPath;

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
    this(name, path, memo, initialItems, false, null);
  }

  /**
   * Creates a project with the given metadata, initial item list, and tutorial flag.
   *
   * @param name directory name (display name)
   * @param path absolute path to the project directory
   * @param memo free-text memo read from {@code .affr_project}; empty if not set
   * @param initialItems the items to populate the list with (copied defensively)
   * @param tutorial {@code true} when this project comes from the bundled tutorial inventory
   */
  public AFFrProject(
      String name,
      Path path,
      String memo,
      List<? extends ProjectItem> initialItems,
      boolean tutorial) {
    this(name, path, memo, initialItems, tutorial, null);
  }

  /**
   * Creates a project with the given metadata, initial item list, tutorial flag, and mirror path.
   *
   * @param name directory name (display name)
   * @param path absolute path to the project directory
   * @param memo free-text memo read from {@code .affr_project}; empty if not set
   * @param initialItems the items to populate the list with (copied defensively)
   * @param tutorial {@code true} when this project comes from the bundled tutorial inventory
   * @param mirrorPath writable directory for user-created calculations in a tutorial project;
   *     {@code null} for regular projects
   */
  public AFFrProject(
      String name,
      Path path,
      String memo,
      List<? extends ProjectItem> initialItems,
      boolean tutorial,
      @Nullable Path mirrorPath) {
    this.name = name;
    this.path = path;
    this.memo = memo;
    this.items = List.copyOf(initialItems);
    this.tutorial = tutorial;
    this.mirrorPath = mirrorPath;
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

  /**
   * {@code true} when this project comes from the bundled tutorial inventory.
   *
   * <p>Tutorial projects use a simplified add-calculation dialog (name only — model settings are
   * defined by the tutorial case) instead of the full model-selection dialog.
   */
  public boolean isTutorial() {
    return tutorial;
  }

  /**
   * Writable mirror directory for user-created calculations, or {@code null} for regular projects.
   *
   * <p>For tutorial projects, new calculations are written here instead of to the read-only
   * tutorial installation directory. {@link #getItems()} already merges calculations loaded from
   * both this directory and the tutorial installation directory.
   */
  public @Nullable Path getMirrorPath() {
    return mirrorPath;
  }
}
