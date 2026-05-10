package affr.project;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ModelConstraints#allowedExtras}.
 *
 * <p>Tests cover base-mode filtering, extra-to-extra compatibility narrowing, and the
 * self-inclusion invariant required to prevent checkboxes from immediately deselecting themselves.
 */
final class ModelConstraintsTest {

  // ── Base-mode filtering (BASIC_ALLOWED) ───────────────────────────────────

  @Test
  void incompressibleSteadyLesAllowsNoExtras() {
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.INCOMPRESSIBLE, SteadyModel.STEADY, TurbModel.LES, Set.of());
    assertTrue(allowed.isEmpty());
  }

  @Test
  void incompressibleSteadyRansAllowsCombustionFamily() {
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.INCOMPRESSIBLE, SteadyModel.STEADY, TurbModel.RANS, Set.of());
    assertTrue(allowed.contains(ExtraModel.COMBUSTION));
    assertTrue(allowed.contains(ExtraModel.COMBUST_CHEM_REACT));
    assertTrue(allowed.contains(ExtraModel.ROTATING_FRAME));
    assertFalse(allowed.contains(ExtraModel.VOF));
    assertFalse(allowed.contains(ExtraModel.MOVING_MESH));
    assertFalse(allowed.contains(ExtraModel.CAVITATION));
  }

  @Test
  void incompressibleUnsteadyRansAllowsVofCavitationMovingMesh() {
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.INCOMPRESSIBLE, SteadyModel.UNSTEADY, TurbModel.RANS, Set.of());
    assertTrue(allowed.contains(ExtraModel.VOF));
    assertTrue(allowed.contains(ExtraModel.MOVING_MESH));
    assertTrue(allowed.contains(ExtraModel.CAVITATION));
  }

  @Test
  void compressibleSteadyLesAllowsOnlyRadiationFamily() {
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.COMPRESSIBLE, SteadyModel.STEADY, TurbModel.LES, Set.of());
    assertTrue(allowed.contains(ExtraModel.RADIATION));
    assertTrue(allowed.contains(ExtraModel.SURFACE_REACTION));
    assertFalse(allowed.contains(ExtraModel.COMBUSTION));
    assertFalse(allowed.contains(ExtraModel.VOF));
  }

  // ── Extra-to-extra compatibility narrowing (EXTRA_COMPATIBLE) ────────────

  @Test
  void selectingVofNarrowsSetToVofAndRotatingFrame() {
    // VOF is compatible with {VOF, ROTATING_FRAME} only; everything else should be removed.
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.INCOMPRESSIBLE,
            SteadyModel.UNSTEADY,
            TurbModel.RANS,
            Set.of(ExtraModel.VOF));
    assertTrue(allowed.contains(ExtraModel.VOF));
    assertTrue(allowed.contains(ExtraModel.ROTATING_FRAME));
    assertFalse(allowed.contains(ExtraModel.COMBUSTION));
    assertFalse(allowed.contains(ExtraModel.COMBUST_CHEM_REACT));
    assertFalse(allowed.contains(ExtraModel.MOVING_MESH));
    assertFalse(allowed.contains(ExtraModel.CAVITATION));
  }

  @Test
  void selectingRotatingFrameKeepsVofCavitationCombustChem() {
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.INCOMPRESSIBLE,
            SteadyModel.UNSTEADY,
            TurbModel.RANS,
            Set.of(ExtraModel.ROTATING_FRAME));
    assertTrue(allowed.contains(ExtraModel.VOF));
    assertTrue(allowed.contains(ExtraModel.CAVITATION));
    assertTrue(allowed.contains(ExtraModel.COMBUST_CHEM_REACT));
    assertTrue(allowed.contains(ExtraModel.ROTATING_FRAME));
  }

  @Test
  void noSelectedExtrasReturnsBasicAllowedForThatMode() {
    // With no extras selected the result is exactly the BASIC_ALLOWED set for the combination.
    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            ComprsModel.INCOMPRESSIBLE, SteadyModel.UNSTEADY, TurbModel.LES, Set.of());
    // incompressible+unsteady: VOF, CAVITATION, COMBUST_CHEM_REACT, COMBUSTION,
    // ROTATING_FRAME, MOVING_MESH
    assertTrue(allowed.contains(ExtraModel.VOF));
    assertTrue(allowed.contains(ExtraModel.MOVING_MESH));
    assertTrue(allowed.contains(ExtraModel.CAVITATION));
    assertFalse(allowed.contains(ExtraModel.RADIATION));
  }

  // ── Self-inclusion invariant ──────────────────────────────────────────────

  @Test
  void everyExtraIncompatibleMapIncludesItself() {
    // If an extra X does not include itself in EXTRA_COMPATIBLE.get(X), then selecting X
    // immediately removes X from the allowed set on the next constraint recompute — a UI bug.
    for (ExtraModel extra : ExtraModel.values()) {
      Set<ExtraModel> compat = ModelConstraints.EXTRA_COMPATIBLE.get(extra);
      if (compat != null) {
        assertTrue(compat.contains(extra), extra + " is missing from its own EXTRA_COMPATIBLE set");
      }
    }
  }
}
