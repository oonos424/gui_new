package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  // ── Tutorial flag ─────────────────────────────────────────────────────────

  @Test
  void isTutorialDefaultsToFalseForFourArgConstructor() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());

    assertFalse(p.isTutorial());
  }

  @Test
  void isTutorialFalseWhenFlagExplicitlyFalse() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(), false);

    assertFalse(p.isTutorial());
  }

  @Test
  void isTutorialTrueWhenFlagSet() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(), true);

    assertTrue(p.isTutorial());
  }

  // ── Mirror path ───────────────────────────────────────────────────────────

  @Test
  void mirrorPathIsNullByDefault() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of());

    assertNull(p.getMirrorPath());
  }

  @Test
  void mirrorPathIsNullWhenNotProvided() {
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(), true);

    assertNull(p.getMirrorPath());
  }

  @Test
  void mirrorPathIsReturnedWhenSet() {
    Path mirror = Path.of("/tmp/.tutorials/my_case");
    AFFrProject p = new AFFrProject("p", PROJ_PATH, "", List.of(), true, mirror);

    assertEquals(mirror, p.getMirrorPath());
  }
}
