package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrProject} plain-data fields. */
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

  @Test
  void itemsListIsDefensivelyCopied() {
    AFFrCalculation item = makeCalc();
    List<AFFrCalculation> mutable = new java.util.ArrayList<>(List.of(item));
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", mutable);

    mutable.clear();

    assertEquals(1, p.getItems().size()); // project's list is unaffected
  }
}
