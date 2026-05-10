package affr.fx.viewmodel.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import affr.project.AFFrCalculationModel;
import affr.project.ComprsModel;
import affr.project.ExtraModel;
import affr.project.SteadyModel;
import affr.project.TurbModel;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link InputTab}.
 *
 * <p>{@link InputTab#tabsFor(AFFrCalculationModel)} is a pure function over an immutable record, so
 * these tests need neither the JavaFX Application Thread nor any test fixture beyond {@link
 * AFFrCalculationModel}.
 */
final class InputTabTest {

  private static final List<InputTab> STANDARD_TABS =
      List.of(
          InputTab.MESH,
          InputTab.MODEL,
          InputTab.FLUID,
          InputTab.BOUNDARY,
          InputTab.SCHEME,
          InputTab.IO_MONITORING);

  private static AFFrCalculationModel modelWith(ExtraModel... extras) {
    return new AFFrCalculationModel(
        ComprsModel.INCOMPRESSIBLE, SteadyModel.STEADY, TurbModel.RANS, Set.of(extras));
  }

  // ── Standard tabs ─────────────────────────────────────────────────────────

  @Test
  void tabsForDefaultModelReturnsSixStandardTabsInDeclaredOrder() {
    List<InputTab> tabs = InputTab.tabsFor(AFFrCalculationModel.DEFAULT);

    assertEquals(STANDARD_TABS, tabs);
  }

  @Test
  void standardTabsAlwaysComeBeforeOptionalTabs() {
    AFFrCalculationModel model =
        modelWith(
            ExtraModel.VOF, ExtraModel.RADIATION, ExtraModel.CAVITATION, ExtraModel.PARTICLE_TRACK);

    List<InputTab> tabs = InputTab.tabsFor(model);

    assertEquals(STANDARD_TABS, tabs.subList(0, 6));
  }

  // ── Single-extra mapping ──────────────────────────────────────────────────

  /**
   * Parameterised exhaustive sweep of all {@link ExtraModel} values. For each, asserts the optional
   * tabs that should appear (after the standard six). The "no extra tab" rows guard against future
   * tabs accidentally getting wired to triggers without an explicit decision.
   */
  @ParameterizedTest(name = "{0} → {1}")
  @MethodSource("singleExtraToOptionalTabs")
  void singleExtraModelTriggersExpectedOptionalTabs(
      ExtraModel extra, List<InputTab> expectedOptional) {
    List<InputTab> tabs = InputTab.tabsFor(modelWith(extra));

    assertEquals(STANDARD_TABS, tabs.subList(0, 6));
    assertEquals(expectedOptional, tabs.subList(6, tabs.size()));
  }

  static List<Arguments> singleExtraToOptionalTabs() {
    return List.of(
        Arguments.of(ExtraModel.COMBUSTION, List.of(InputTab.COMBUSTION)),
        Arguments.of(
            ExtraModel.COMBUST_CHEM_REACT, List.of(InputTab.COMBUSTION, InputTab.REACTION)),
        Arguments.of(ExtraModel.PARTICLE_TRACK, List.of(InputTab.PARTICLE_TRACK)),
        Arguments.of(ExtraModel.RADIATION, List.of(InputTab.RADIATION)),
        Arguments.of(ExtraModel.CAVITATION, List.of(InputTab.CAVITATION)),
        Arguments.of(ExtraModel.VOF, List.of(InputTab.VOF)),
        Arguments.of(ExtraModel.GHOST_FLUID, List.of(InputTab.VOF)),
        Arguments.of(ExtraModel.SURFACE_REACTION, List.of()),
        Arguments.of(ExtraModel.POROUS_MODEL, List.of()),
        Arguments.of(ExtraModel.ROTATING_FRAME, List.of()),
        Arguments.of(ExtraModel.MOVING_MESH, List.of()),
        Arguments.of(ExtraModel.OVERSET_GRID, List.of()));
  }

  // ── Multi-extra cases ─────────────────────────────────────────────────────

  /**
   * The most likely off-by-one in the visibility logic: {@link ExtraModel#COMBUSTION} on its own
   * must NOT add the {@link InputTab#REACTION} tab, even though {@link
   * ExtraModel#COMBUST_CHEM_REACT} does.
   */
  @Test
  void combustionAloneDoesNotAddReactionTab() {
    List<InputTab> tabs = InputTab.tabsFor(modelWith(ExtraModel.COMBUSTION));

    assertEquals(List.of(InputTab.COMBUSTION), tabs.subList(6, tabs.size()));
  }

  @Test
  void combustChemReactAddsBothCombustionAndReactionTabs() {
    List<InputTab> tabs = InputTab.tabsFor(modelWith(ExtraModel.COMBUST_CHEM_REACT));

    assertEquals(List.of(InputTab.COMBUSTION, InputTab.REACTION), tabs.subList(6, tabs.size()));
  }

  @Test
  void vofAndGhostFluidTogetherProduceSingleVofTab() {
    List<InputTab> tabs = InputTab.tabsFor(modelWith(ExtraModel.VOF, ExtraModel.GHOST_FLUID));

    assertEquals(List.of(InputTab.VOF), tabs.subList(6, tabs.size()));
  }

  /**
   * Optional tabs must come out in {@link InputTab} declaration order, not the order in which the
   * triggering extras were added to the set or the order of {@link ExtraModel} declaration.
   */
  @Test
  void multipleExtrasProduceTabsInInputTabDeclarationOrder() {
    AFFrCalculationModel model =
        modelWith(ExtraModel.VOF, ExtraModel.COMBUSTION, ExtraModel.CAVITATION);

    List<InputTab> tabs = InputTab.tabsFor(model);

    assertEquals(
        List.of(InputTab.COMBUSTION, InputTab.CAVITATION, InputTab.VOF),
        tabs.subList(6, tabs.size()));
  }

  // ── Non-extra fields don't influence visibility ───────────────────────────

  /**
   * Pins that visibility depends only on {@link AFFrCalculationModel#extraModelSet()}. If a future
   * implementer makes a tab depend on (e.g.) {@link ComprsModel}, this test fails and forces an
   * explicit update of the contract.
   */
  @Test
  void compressibilityAndSteadyAndTurbDoNotAffectTabVisibility() {
    AFFrCalculationModel base =
        new AFFrCalculationModel(
            ComprsModel.INCOMPRESSIBLE,
            SteadyModel.STEADY,
            TurbModel.RANS,
            Set.of(ExtraModel.COMBUSTION));
    AFFrCalculationModel altered =
        new AFFrCalculationModel(
            ComprsModel.COMPRESSIBLE,
            SteadyModel.UNSTEADY,
            TurbModel.LES,
            Set.of(ExtraModel.COMBUSTION));

    assertEquals(InputTab.tabsFor(base), InputTab.tabsFor(altered));
  }

  // ── Result independence ──────────────────────────────────────────────────

  /**
   * Each call to {@link InputTab#tabsFor(AFFrCalculationModel)} must return a fresh list — the
   * controller relies on this when it iterates over the tabs and stores them per-tab.
   */
  @Test
  void tabsForReturnsFreshListEachCall() {
    AFFrCalculationModel model = AFFrCalculationModel.DEFAULT;

    List<InputTab> first = InputTab.tabsFor(model);
    List<InputTab> second = InputTab.tabsFor(model);

    assertNotNull(first);
    assertNotNull(second);
    assertEquals(first, second);
    assertNotSame(first, second);
  }

  // ── Label keys ────────────────────────────────────────────────────────────

  @Test
  void everyTabHasNonEmptyLabelKey() {
    for (InputTab tab : InputTab.values()) {
      String key = tab.labelKey();
      assertNotNull(key, () -> tab + " has null labelKey");
      assertEquals(false, key.isEmpty(), () -> tab + " has empty labelKey");
    }
  }

  @Test
  void labelKeysAreUniqueAcrossTabs() {
    InputTab[] values = InputTab.values();
    Set<String> keys = Set.copyOf(java.util.Arrays.stream(values).map(InputTab::labelKey).toList());
    assertEquals(values.length, keys.size(), "two InputTab values share a labelKey");
  }
}
