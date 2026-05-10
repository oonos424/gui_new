package affr.fx.viewmodel.top.file;

import affr.project.AFFrCalculation;
import affr.project.AFFrProject;
import affr.project.ProjectItem;
import java.nio.file.Path;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ViewModel for the Project Item List screen (Calculation List).
 *
 * <p>Wraps an {@link AFFrProject} and exposes observable state that the View layer binds to
 * directly: a live {@link ObservableList} of items (populated from the model at construction), a
 * {@link SortedList} view of that list driven by a {@link ProjectSortOrder} property, and an
 * observable focused-item selection.
 *
 * <p>Owns the per-view-session focused-item selection, which is presentation state (which item the
 * user is currently looking at) and intentionally not part of {@link AFFrProject}. Two ViewModels
 * over the same project may therefore track focus independently.
 *
 * <p>This class holds no widget references and no FXML knowledge.
 *
 * <p>Transient focused-item state is not persisted in this phase; it will be restored from {@code
 * .current_focus} when the Input Editor phase adds that persistence.
 */
public final class ProjectViewModel {

  private final AFFrProject project;
  private final ObservableList<ProjectItem> items;
  private final SortedList<ProjectItem> sortedItems;
  private final ObjectProperty<ProjectSortOrder> sortOrder =
      new SimpleObjectProperty<>(ProjectSortOrder.DATE_DESC);

  // Transient: not persisted in this phase.
  private final ObjectProperty<@Nullable ProjectItem> focusedItem =
      new SimpleObjectProperty<>(null);

  // Transient: drives navigation into the Input Editor when set, then cleared by the listener.
  private final ObjectProperty<@Nullable AFFrCalculation> openCalculationRequest =
      new SimpleObjectProperty<>(null);

  /**
   * Creates a ViewModel backed by {@code project}. The item list is copied from the model into a
   * new {@link ObservableList}; subsequent mutations go through this ViewModel, not the model. The
   * sort order starts at {@link ProjectSortOrder#DATE_DESC} and the focused item starts at {@code
   * null}.
   */
  public ProjectViewModel(AFFrProject project) {
    this.project = project;
    this.items = FXCollections.observableArrayList(project.getItems());
    this.sortedItems = new SortedList<>(items);
    sortedItems.setComparator(sortOrder.get().comparator());
    sortOrder.addListener((obs, old, order) -> sortedItems.setComparator(order.comparator()));
  }

  // ── Items ─────────────────────────────────────────────────────────────────

  /**
   * The sorted view of the project's items. The backing list is live — additions or removals via
   * {@link #addItem}, {@link #removeItem}, and {@link #replaceItem} are immediately reflected here
   * in the current sort order.
   */
  public SortedList<ProjectItem> getSortedItems() {
    return sortedItems;
  }

  // ── Sort order ────────────────────────────────────────────────────────────

  public ObjectProperty<ProjectSortOrder> sortOrderProperty() {
    return sortOrder;
  }

  public ProjectSortOrder getSortOrder() {
    return sortOrder.get();
  }

  public void setSortOrder(ProjectSortOrder order) {
    sortOrder.set(order);
  }

  // ── Project access ────────────────────────────────────────────────────────

  /** The underlying domain project. Exposed for use by the app layer (e.g. creating items). */
  public AFFrProject getProject() {
    return project;
  }

  /** The project's display name (directory name). */
  public String getProjectName() {
    return project.getName();
  }

  /** Absolute path to the project directory on disk. */
  public Path getProjectPath() {
    return project.getPath();
  }

  /** Free-text memo from {@code .affr_project}; empty string if not set. */
  public String getProjectMemo() {
    return project.getMemo();
  }

  /** The live observable list of project items (unsorted). Used for mutations such as add. */
  public ObservableList<ProjectItem> getProjectItems() {
    return items;
  }

  /** Adds {@code item} to the item list. Must be called on the JavaFX Application Thread. */
  public void addItem(ProjectItem item) {
    items.add(item);
  }

  /** Removes {@code item} from the item list. Must be called on the JavaFX Application Thread. */
  public void removeItem(ProjectItem item) {
    items.remove(item);
  }

  /**
   * Replaces {@code old} with {@code replacement} in the item list, preserving position. If {@code
   * old} is not found, {@code replacement} is appended. Must be called on the JavaFX Application
   * Thread.
   */
  public void replaceItem(ProjectItem old, ProjectItem replacement) {
    int index = items.indexOf(old);
    if (index >= 0) {
      items.set(index, replacement);
    } else {
      items.add(replacement);
    }
  }

  // ── Focused item ──────────────────────────────────────────────────────────

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

  // ── Open-calculation request ─────────────────────────────────────────────

  /**
   * One-shot signal: the View layer sets this property to an {@link AFFrCalculation} when the user
   * asks to open it (e.g. by double-clicking the item list). The navigation layer subscribes to
   * this property, opens the Input Editor, and then calls {@link #clearOpenCalculationRequest()} to
   * acknowledge the signal — allowing the same calculation to be re-opened later.
   */
  public ObjectProperty<@Nullable AFFrCalculation> openCalculationRequestProperty() {
    return openCalculationRequest;
  }

  /** Asks the navigation layer to open {@code calculation} in the Input Editor. */
  public void requestOpenCalculation(AFFrCalculation calculation) {
    openCalculationRequest.set(calculation);
  }

  /** Acknowledges the most recent open-calculation request. */
  public void clearOpenCalculationRequest() {
    openCalculationRequest.set(null);
  }
}
