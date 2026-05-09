package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.collections.ListChangeListener;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AFFrProject}.
 *
 * <p>JavaFX properties ({@link javafx.beans.property.SimpleObjectProperty}, {@link
 * javafx.collections.FXCollections#observableArrayList}) work without the JavaFX Application Thread
 * for plain reads, writes, and listener notifications, so no TestFX is required.
 */
final class AFFrProjectTest {

  private static final Path PROJ_PATH = Path.of("/tmp/test_project");
  private static final Path CAL_PATH = PROJ_PATH.resolve("cal_01");

  private static AFFrCalculation makeCalc() {
    return new AFFrCalculation(
        "cal_01", CAL_PATH, null, AFFrCalProperty.DEFAULT, AFFrCalculationModel.DEFAULT);
  }

  // ── Construction ──────────────────────────────────────────────────────────

  @Test
  void constructorPreservesAllFields() {
    List<AFFrCalculation> items = List.of(makeCalc());
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
    AFFrCalculation item = makeCalc();
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(item));

    assertEquals(1, p.getItems().size());
    assertSame(item, p.getItems().get(0));
  }

  // ── Focused item ──────────────────────────────────────────────────────────

  @Test
  void focusedItemIsNullInitially() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());
    assertNull(p.getFocusedItem());
    assertNull(p.focusedItemProperty().get());
  }

  @Test
  void setFocusedItemUpdatesProperty() {
    AFFrCalculation item = makeCalc();
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(item));

    p.setFocusedItem(item);

    assertSame(item, p.getFocusedItem());
    assertSame(item, p.focusedItemProperty().get());
  }

  @Test
  void focusedItemCanBeResetToNull() {
    AFFrCalculation item = makeCalc();
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(item));
    p.setFocusedItem(item);

    p.setFocusedItem(null);

    assertNull(p.getFocusedItem());
  }

  @Test
  void focusedItemListenerFiresOnChange() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());
    AtomicInteger calls = new AtomicInteger();
    p.focusedItemProperty().addListener((obs, o, n) -> calls.incrementAndGet());

    p.setFocusedItem(makeCalc());
    p.setFocusedItem(null);

    assertEquals(2, calls.get());
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

    p.getItems().add(makeCalc());

    assertEquals(1, added.size());
    assertSame(p.getItems().get(0), added.get(0));
  }
}
