package affr.fx.viewmodel.top.file;

import affr.project.AFFrProject;
import affr.project.ProjectItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.SortedList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ViewModel for the Project Item List screen (Calculation List).
 *
 * <p>Wraps an {@link AFFrProject} and exposes a {@link SortedList} view of its items, driven by a
 * {@link ProjectSortOrder} property. Changing the sort order re-sorts the list live — no reload
 * needed.
 *
 * <p>This class holds no widget references and no FXML knowledge.
 */
public final class ProjectViewModel {

  private final AFFrProject project;
  private final SortedList<ProjectItem> sortedItems;
  private final ObjectProperty<ProjectSortOrder> sortOrder =
      new SimpleObjectProperty<>(ProjectSortOrder.DATE_DESC);

  /**
   * Creates a ViewModel backed by {@code project}. The sort order starts at {@link
   * ProjectSortOrder#DATE_DESC}.
   */
  public ProjectViewModel(AFFrProject project) {
    this.project = project;
    this.sortedItems = new SortedList<>(project.getItems());
    sortedItems.setComparator(sortOrder.get().comparator());
    sortOrder.addListener((obs, old, order) -> sortedItems.setComparator(order.comparator()));
  }

  // ── Items ─────────────────────────────────────────────────────────────────

  /**
   * The sorted view of the project's items. The backing list is live — additions or removals to the
   * project's item list are immediately reflected here, in the current sort order.
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

  /** The project's display name (directory name). */
  public String getProjectName() {
    return project.getName();
  }

  /** Free-text memo from {@code .affr_project}; empty string if not set. */
  public String getProjectMemo() {
    return project.getMemo();
  }

  // ── Focused item (forwarded from AFFrProject) ─────────────────────────────

  public ObjectProperty<@Nullable ProjectItem> focusedItemProperty() {
    return project.focusedItemProperty();
  }

  public @Nullable ProjectItem getFocusedItem() {
    return project.getFocusedItem();
  }

  public void setFocusedItem(@Nullable ProjectItem item) {
    project.setFocusedItem(item);
  }
}
