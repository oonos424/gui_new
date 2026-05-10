package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AFFrProject}.
 *
 * <p>JavaFX collections ({@link javafx.collections.FXCollections#observableArrayList}) work without
 * the JavaFX Application Thread for plain reads, writes, and listener notifications, so no TestFX
 * is required.
 *
 * <p>Per-view-session selection state (the focused item) lives in {@code ProjectViewModel} and is
 * covered by {@code ProjectViewModelTest}; it is intentionally not part of {@code AFFrProject}.
 */
final class AFFrProjectTest {

  private static final Path PROJ_PATH = Path.of("/tmp/test_project");
  private static final Path CAL_PATH = PROJ_PATH.resolve("cal_01");

  private static CalculationItem makeCalItem() {
    return new CalculationItem("cal_01", CAL_PATH, CalculationStatus.SETTING, "2026-05-01");
  }

  // ── Construction ──────────────────────────────────────────────────────────

  @Test
  void constructorPreservesAllFields() {
    List<ProjectItem> items = List.of(makeCalItem());
    AFFrProject p = new AFFrProject("my_project", PROJ_PATH, "memo text", items);

    assertEquals("my_project", p.getName());
    assertEquals(PROJ_PATH, p.getPath());
    assertEquals("memo text", p.getMemo());
    assertEquals(1, p.getItems().size());
  }

  @Test
  void emptyMemoIsAllowed() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());
    assertEquals("", p.getMemo());
  }

  @Test
  void itemsArePopulatedFromInitialList() {
    CalculationItem item = makeCalItem();
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(item));

    assertEquals(1, p.getItems().size());
    assertSame(item, p.getItems().get(0));
  }

  // ── Observable list ───────────────────────────────────────────────────────

  @Test
  void getItemsReturnsSameInstanceEveryCall() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());
    assertSame(p.getItems(), p.getItems());
  }

  @Test
  void itemsListIsLiveObservable() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());
    List<ProjectItem> added = new ArrayList<>();
    p.getItems()
        .addListener(
            (ListChangeListener<ProjectItem>)
                c -> {
                  while (c.next()) {
                    added.addAll(c.getAddedSubList());
                  }
                });

    p.getItems().add(makeCalItem());

    assertEquals(1, added.size());
    assertSame(p.getItems().get(0), added.get(0));
  }
}
