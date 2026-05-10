package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrCalculationModel}. */
final class AFFrCalculationModelTest {

  // ── DEFAULT constant ───────────────────────────────────────────────────────

  @Test
  void defaultComprsModelIsIncompressible() {
    assertEquals(ComprsModel.INCOMPRESSIBLE, AFFrCalculationModel.DEFAULT.comprsModel());
  }

  @Test
  void defaultSteadyModelIsSteady() {
    assertEquals(SteadyModel.STEADY, AFFrCalculationModel.DEFAULT.steadyModel());
  }

  @Test
  void defaultTurbModelIsRans() {
    assertEquals(TurbModel.RANS, AFFrCalculationModel.DEFAULT.turbModel());
  }

  @Test
  void defaultExtraModelSetIsEmpty() {
    assertTrue(AFFrCalculationModel.DEFAULT.extraModelSet().isEmpty());
  }

  // ── Record construction ────────────────────────────────────────────────────

  @Test
  void constructorPreservesAllFields() {
    Set<ExtraModel> extras = Set.of(ExtraModel.VOF, ExtraModel.RADIATION);

    AFFrCalculationModel m =
        new AFFrCalculationModel(
            ComprsModel.COMPRESSIBLE, SteadyModel.UNSTEADY, TurbModel.LES, extras);

    assertEquals(ComprsModel.COMPRESSIBLE, m.comprsModel());
    assertEquals(SteadyModel.UNSTEADY, m.steadyModel());
    assertEquals(TurbModel.LES, m.turbModel());
    assertEquals(extras, m.extraModelSet());
  }

  @Test
  void allExtraModelValuesAreUnique() {
    ExtraModel[] values = ExtraModel.values();
    Set<ExtraModel> asSet = Set.of(values);
    assertEquals(values.length, asSet.size());
  }
}
