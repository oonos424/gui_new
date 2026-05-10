package affr.fx.viewmodel.top.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import affr.project.AFFrProject;
import affr.project.CalculationItem;
import affr.project.CalculationStatus;
import affr.project.ProjectItem;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProjectViewModel}.
 *
 * <p>Focuses on behaviour that lives in the ViewModel layer rather than the domain layer: the
 * focused-item property (formerly on {@link AFFrProject}) and the sort-order property's effect on
 * the exposed {@link javafx.collections.transformation.SortedList}.
 *
 * <p>JavaFX properties used here ({@link javafx.beans.property.SimpleObjectProperty}, {@link
 * javafx.collections.transformation.SortedList}) operate without the JavaFX Application Thread for
 * plain reads, writes, and listener notifications, so no TestFX is required.
 */
final class ProjectViewModelTest {

  private static final Path PROJ_PATH = Path.of("/tmp/test_project");

  private static CalculationItem cal(String name, String date) {
    return new CalculationItem(name, PROJ_PATH.resolve(name), CalculationStatus.SETTING, date);
  }

  private static AFFrProject project(List<ProjectItem> items) {
    return new AFFrProject("p", PROJ_PATH, "", items);
  }

  // ── Focused item ──────────────────────────────────────────────────────────

  @Test
  void focusedItemIsNullInitially() {
    ProjectViewModel vm = new ProjectViewModel(project(List.of()));

    assertNull(vm.getFocusedItem());
    assertNull(vm.focusedItemProperty().get());
  }

  @Test
  void setFocusedItemUpdatesProperty() {
    CalculationItem item = cal("cal_01", "2026-05-01");
    ProjectViewModel vm = new ProjectViewModel(project(List.of(item)));

    vm.setFocusedItem(item);

    assertSame(item, vm.getFocusedItem());
    assertSame(item, vm.focusedItemProperty().get());
  }

  @Test
  void focusedItemCanBeResetToNull() {
    CalculationItem item = cal("cal_01", "2026-05-01");
    ProjectViewModel vm = new ProjectViewModel(project(List.of(item)));
    vm.setFocusedItem(item);

    vm.setFocusedItem(null);

    assertNull(vm.getFocusedItem());
  }

  @Test
  void focusedItemListenerFiresOnChange() {
    CalculationItem item = cal("cal_01", "2026-05-01");
    ProjectViewModel vm = new ProjectViewModel(project(List.of(item)));
    AtomicInteger calls = new AtomicInteger();
    vm.focusedItemProperty().addListener((obs, o, n) -> calls.incrementAndGet());

    vm.setFocusedItem(item);
    vm.setFocusedItem(null);

    assertEquals(2, calls.get());
  }

  /**
   * Two ViewModels over the same project must track focus independently. This is the whole reason
   * focusedItem moved out of {@link AFFrProject} into {@link ProjectViewModel}.
   */
  @Test
  void focusIsIndependentBetweenTwoViewModelsOverTheSameProject() {
    CalculationItem itemA = cal("cal_01", "2026-05-01");
    CalculationItem itemB = cal("cal_02", "2026-05-02");
    AFFrProject p = project(List.of(itemA, itemB));

    ProjectViewModel vm1 = new ProjectViewModel(p);
    ProjectViewModel vm2 = new ProjectViewModel(p);

    vm1.setFocusedItem(itemA);
    vm2.setFocusedItem(itemB);

    assertSame(itemA, vm1.getFocusedItem());
    assertSame(itemB, vm2.getFocusedItem());
  }

  // ── Sort order ────────────────────────────────────────────────────────────

  @Test
  void defaultSortOrderIsDateDesc() {
    ProjectViewModel vm = new ProjectViewModel(project(List.of()));

    assertSame(ProjectSortOrder.DATE_DESC, vm.getSortOrder());
  }

  @Test
  void itemsAreSortedByDefaultSortOrderInitially() {
    ProjectItem a = cal("a", "2026-01-01");
    ProjectItem b = cal("b", "2026-05-01");
    ProjectViewModel vm = new ProjectViewModel(project(List.of(a, b)));

    // DATE_DESC: newer first
    assertEquals(List.of(b, a), List.copyOf(vm.getSortedItems()));
  }

  @Test
  void changingSortOrderResortsItemsLive() {
    ProjectItem a = cal("a", "2026-01-01");
    ProjectItem b = cal("b", "2026-05-01");
    ProjectViewModel vm = new ProjectViewModel(project(List.of(a, b)));

    vm.setSortOrder(ProjectSortOrder.NAME_ASC);

    assertEquals(List.of(a, b), List.copyOf(vm.getSortedItems()));
  }

  // ── Project pass-through ──────────────────────────────────────────────────

  @Test
  void projectNameIsExposed() {
    ProjectViewModel vm = new ProjectViewModel(project(List.of()));

    assertEquals("p", vm.getProjectName());
  }

  @Test
  void projectMemoIsExposed() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "memo text", List.of());
    ProjectViewModel vm = new ProjectViewModel(p);

    assertEquals("memo text", vm.getProjectMemo());
  }
}
