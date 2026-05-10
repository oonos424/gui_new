package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AFFrCalculation}.
 *
 * <p>Tests cover construction, {@link ProjectItem} interface delegation, convenience accessors, and
 * property/model mutability.
 */
final class AFFrCalculationTest {

  private static final Path CAL_PATH = Path.of("/tmp/proj/cal_01");

  private static AFFrCalculation makeCalc() {
    return new AFFrCalculation(
        "cal_01", CAL_PATH, null, AFFrCalProperty.DEFAULT, AFFrCalculationModel.DEFAULT);
  }

  // ── ProjectItem interface ─────────────────────────────────────────────────

  @Test
  void nameMatchesConstructorArg() {
    assertEquals("cal_01", makeCalc().name());
  }

  @Test
  void pathMatchesConstructorArg() {
    assertEquals(CAL_PATH, makeCalc().path());
  }

  @Test
  void dateDelegatesToProperty() {
    AFFrCalProperty prop =
        new AFFrCalProperty(
            CalculationStatus.SETTING,
            "2026-05-01",
            0,
            "localhost",
            "",
            "未設定",
            1,
            false,
            Map.of(),
            Map.of());
    AFFrCalculation c =
        new AFFrCalculation("cal_01", CAL_PATH, null, prop, AFFrCalculationModel.DEFAULT);

    assertEquals("2026-05-01", c.date());
  }

  @Test
  void dateFallsBackToEmptyStringFromDefault() {
    assertEquals("", makeCalc().date());
  }

  // ── Convenience accessors ─────────────────────────────────────────────────

  @Test
  void getStatusDelegatesToProperty() {
    assertEquals(CalculationStatus.SETTING, makeCalc().getStatus());
  }

  @Test
  void getStatusReflectsPropertyStatus() {
    AFFrCalProperty prop =
        new AFFrCalProperty(
            CalculationStatus.CALCULATED,
            "",
            0,
            "localhost",
            "",
            "未設定",
            1,
            false,
            Map.of(),
            Map.of());
    AFFrCalculation c =
        new AFFrCalculation("cal_01", CAL_PATH, null, prop, AFFrCalculationModel.DEFAULT);

    assertEquals(CalculationStatus.CALCULATED, c.getStatus());
  }

  // ── Mutable property ──────────────────────────────────────────────────────

  @Test
  void setPropertyUpdatesDateAndStatus() {
    AFFrCalculation c = makeCalc();
    AFFrCalProperty newProp =
        new AFFrCalProperty(
            CalculationStatus.CALCULATED,
            "2026-05-10",
            100,
            "localhost",
            "",
            "未設定",
            1,
            false,
            Map.of(),
            Map.of());

    c.setProperty(newProp);

    assertEquals(CalculationStatus.CALCULATED, c.getStatus());
    assertEquals("2026-05-10", c.date());
  }

  // ── Mutable model ─────────────────────────────────────────────────────────

  @Test
  void setModelUpdatesModel() {
    AFFrCalculation c = makeCalc();
    AFFrCalculationModel newModel =
        new AFFrCalculationModel(
            ComprsModel.COMPRESSIBLE, SteadyModel.UNSTEADY, TurbModel.LES, java.util.Set.of());

    c.setModel(newModel);

    assertEquals(ComprsModel.COMPRESSIBLE, c.getModel().comprsModel());
    assertEquals(SteadyModel.UNSTEADY, c.getModel().steadyModel());
    assertEquals(TurbModel.LES, c.getModel().turbModel());
  }

  // ── Project back-reference ────────────────────────────────────────────────

  @Test
  void getProjectThrowsIfNotSet() {
    AFFrCalculation c = makeCalc();
    assertThrows(IllegalStateException.class, c::getProject);
  }

  @Test
  void setProjectThenGetProjectReturnsIt() {
    AFFrCalculation c = makeCalc();
    AFFrProject project = new AFFrProject("proj", Path.of("/tmp/proj"), "", java.util.List.of());

    c.setProject(project);

    assertEquals(project, c.getProject());
  }
}
