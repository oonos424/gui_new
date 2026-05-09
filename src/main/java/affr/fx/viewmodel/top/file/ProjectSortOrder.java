package affr.fx.viewmodel.top.file;

import affr.project.ProjectItem;
import java.util.Comparator;

/**
 * Sort orders available in the project item list.
 *
 * <p>Each constant carries a ready-made {@link Comparator} so the ViewModel can wire it directly to
 * a {@link javafx.collections.transformation.SortedList} without any switch logic at the call site.
 * New sort keys are added as new constants; existing callers are unaffected.
 *
 * <p>{@code DATE_DESC} (newest first) is the default — the most-recently-modified item appears at
 * the top, which makes the last-worked-on calculation immediately visible.
 */
public enum ProjectSortOrder {
  DATE_DESC("sort.dateDesc", Comparator.comparing(ProjectItem::date).reversed()),
  DATE_ASC("sort.dateAsc", Comparator.comparing(ProjectItem::date)),
  NAME_ASC("sort.nameAsc", Comparator.comparing(ProjectItem::name, String.CASE_INSENSITIVE_ORDER)),
  NAME_DESC(
      "sort.nameDesc",
      Comparator.comparing(ProjectItem::name, String.CASE_INSENSITIVE_ORDER).reversed());

  private final String messageKey;
  private final Comparator<ProjectItem> comparator;

  ProjectSortOrder(String messageKey, Comparator<ProjectItem> comparator) {
    this.messageKey = messageKey;
    this.comparator = comparator;
  }

  /** Returns the resource-bundle key for the display label (e.g. {@code "sort.dateDesc"}). */
  public String messageKey() {
    return messageKey;
  }

  /** Returns the comparator that implements this sort order. */
  public Comparator<ProjectItem> comparator() {
    return comparator;
  }
}
