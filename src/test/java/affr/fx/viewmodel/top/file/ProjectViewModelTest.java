package affr.fx.viewmodel.top.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.project.AFFrCalProperty;
import affr.project.AFFrCalculation;
import affr.project.AFFrCalculationModel;
import affr.project.AFFrProject;
import affr.project.ProjectItem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.collections.ListChangeListener;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProjectViewModel}.
 *
 * <p>JavaFX observable lists and properties work without the JavaFX Application Thread for plain
 * reads, writes, and listener notifications, so no TestFX or Platform.runLater is required.
 */
final class ProjectViewModelTest {

  private static final Path PROJ_PATH = Path.of("/tmp/vm_test_project");

  private static AFFrCalculation makeCal(String name) {
    return new AFFrCalculation(
        name, PROJ_PATH.resolve(name), null, AFFrCalProperty.DEFAULT, AFFrCalculationModel.DEFAULT);
  }

  private static ProjectViewModel vm(AFFrCalculation... items) {
    AFFrProject project = new AFFrProject("p", PROJ_PATH, "", List.of(items));
    return new ProjectViewModel(project);
  }

  // ── removeItem ─────────────────────────────────────────────────────────────

  @Test
  void removeItemRemovesFromProjectItems() {
    AFFrCalculation cal = makeCal("cal_01");
    ProjectViewModel viewModel = vm(cal);

    viewModel.removeItem(cal);

    assertTrue(viewModel.getProjectItems().isEmpty());
  }

  @Test
  void removeItemIsReflectedInSortedView() {
    AFFrCalculation cal = makeCal("cal_01");
    ProjectViewModel viewModel = vm(cal);

    viewModel.removeItem(cal);

    assertTrue(viewModel.getSortedItems().isEmpty());
  }

  @Test
  void removeItemDoesNotAffectOtherItems() {
    AFFrCalculation cal1 = makeCal("cal_01");
    AFFrCalculation cal2 = makeCal("cal_02");
    ProjectViewModel viewModel = vm(cal1, cal2);

    viewModel.removeItem(cal1);

    assertEquals(1, viewModel.getProjectItems().size());
    assertSame(cal2, viewModel.getProjectItems().get(0));
  }

  @Test
  void removeItemDoesNothingIfItemNotPresent() {
    AFFrCalculation cal = makeCal("cal_01");
    AFFrCalculation other = makeCal("cal_99");
    ProjectViewModel viewModel = vm(cal);

    viewModel.removeItem(other); // not in the list

    assertEquals(1, viewModel.getProjectItems().size());
  }

  // ── replaceItem ────────────────────────────────────────────────────────────

  @Test
  void replaceItemSwapsItemAtSamePosition() {
    AFFrCalculation cal1 = makeCal("cal_01");
    AFFrCalculation cal2 = makeCal("cal_02");
    AFFrCalculation replacement = makeCal("cal_01_renamed");
    ProjectViewModel viewModel = vm(cal1, cal2);

    viewModel.replaceItem(cal1, replacement);

    List<ProjectItem> items = viewModel.getProjectItems();
    assertEquals(2, items.size());
    assertSame(replacement, items.get(0));
    assertSame(cal2, items.get(1));
  }

  @Test
  void replaceItemPreservesListSize() {
    AFFrCalculation cal = makeCal("cal_01");
    AFFrCalculation replacement = makeCal("cal_renamed");
    ProjectViewModel viewModel = vm(cal);

    viewModel.replaceItem(cal, replacement);

    assertEquals(1, viewModel.getProjectItems().size());
  }

  @Test
  void replaceItemIsReflectedInSortedView() {
    AFFrCalculation cal = makeCal("cal_01");
    AFFrCalculation replacement = makeCal("cal_renamed");
    ProjectViewModel viewModel = vm(cal);

    viewModel.replaceItem(cal, replacement);

    assertTrue(viewModel.getSortedItems().contains(replacement));
  }

  @Test
  void replaceItemAppendsIfOldItemNotFound() {
    AFFrCalculation cal = makeCal("cal_01");
    AFFrCalculation stranger = makeCal("cal_99");
    AFFrCalculation replacement = makeCal("cal_renamed");
    ProjectViewModel viewModel = vm(cal);

    viewModel.replaceItem(stranger, replacement); // stranger is not in the list

    assertEquals(2, viewModel.getProjectItems().size());
    assertTrue(viewModel.getProjectItems().contains(replacement));
  }

  // ── Observable list ────────────────────────────────────────────────────────

  @Test
  void getProjectItemsReturnsSameInstanceEveryCall() {
    ProjectViewModel viewModel = vm();
    assertSame(viewModel.getProjectItems(), viewModel.getProjectItems());
  }

  @Test
  void projectItemsIsPopulatedFromModel() {
    AFFrCalculation cal = makeCal("cal_01");
    ProjectViewModel viewModel = vm(cal);

    assertEquals(1, viewModel.getProjectItems().size());
    assertSame(cal, viewModel.getProjectItems().get(0));
  }

  @Test
  void addItemFiresListChangeListener() {
    ProjectViewModel viewModel = vm();
    List<ProjectItem> added = new ArrayList<>();
    viewModel
        .getProjectItems()
        .addListener(
            (ListChangeListener<ProjectItem>)
                c -> {
                  while (c.next()) added.addAll(c.getAddedSubList());
                });

    AFFrCalculation cal = makeCal("cal_01");
    viewModel.addItem(cal);

    assertEquals(1, added.size());
    assertSame(cal, added.get(0));
  }

  // ── Focused item ───────────────────────────────────────────────────────────

  @Test
  void focusedItemIsNullInitially() {
    ProjectViewModel viewModel = vm();
    assertNull(viewModel.getFocusedItem());
    assertNull(viewModel.focusedItemProperty().get());
  }

  @Test
  void setFocusedItemUpdatesProperty() {
    AFFrCalculation cal = makeCal("cal_01");
    ProjectViewModel viewModel = vm(cal);

    viewModel.setFocusedItem(cal);

    assertSame(cal, viewModel.getFocusedItem());
    assertSame(cal, viewModel.focusedItemProperty().get());
  }

  @Test
  void focusedItemCanBeResetToNull() {
    AFFrCalculation cal = makeCal("cal_01");
    ProjectViewModel viewModel = vm(cal);
    viewModel.setFocusedItem(cal);

    viewModel.setFocusedItem(null);

    assertNull(viewModel.getFocusedItem());
  }

  @Test
  void focusedItemListenerFiresOnChange() {
    ProjectViewModel viewModel = vm();
    AtomicInteger calls = new AtomicInteger();
    viewModel.focusedItemProperty().addListener((obs, o, n) -> calls.incrementAndGet());

    viewModel.setFocusedItem(makeCal("cal_01"));
    viewModel.setFocusedItem(null);

    assertEquals(2, calls.get());
  }
}
