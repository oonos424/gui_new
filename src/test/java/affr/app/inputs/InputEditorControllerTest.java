package affr.app.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.fx.viewmodel.inputs.InputTab;
import affr.project.AFFrCalProperty;
import affr.project.AFFrCalculation;
import affr.project.AFFrCalculationModel;
import affr.project.ComprsModel;
import affr.project.ExtraModel;
import affr.project.SteadyModel;
import affr.project.TurbModel;
import affr.util.i18n.I18n;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * View↔ViewModel integration tests for {@link InputEditorController}.
 *
 * <p>Covers the contract the controller is supposed to enforce:
 *
 * <ul>
 *   <li>tab toggles are produced one-to-one with {@link InputTab#tabsFor(AFFrCalculationModel)}, in
 *       order;
 *   <li>the first tab is selected, exactly one tab is always selected, content swaps faithfully;
 *   <li>locale changes refresh tab labels.
 * </ul>
 *
 * <p>Runs headlessly on Monocle — see {@code build.gradle.kts} {@code tasks.test}.
 *
 * <p>Each test re-loads the FXML from scratch via {@link #loadEditor(FxRobot, AFFrCalculation)}.
 * Runnable)} so model-specific tab populations don't leak between tests.
 */
@ExtendWith(ApplicationExtension.class)
final class InputEditorControllerTest {

  private static final Path PROJ_PATH = Path.of("/tmp/input_editor_test_project");

  private Stage stage;

  @Start
  void start(Stage stage) {
    this.stage = stage;
  }

  @AfterEach
  void resetLocale(FxRobot robot) {
    robot.interact(() -> I18n.setLocale(Locale.ENGLISH));
  }

  // ── Tab population ────────────────────────────────────────────────────────

  @Test
  void fxmlInjectsRequiredWidgets(FxRobot robot) {
    Loaded loaded = loadEditor(robot, calculationWithDefaultModel());

    assertNotNull(loaded.tabButtonsBox(), "tabButtons must be injected by FXMLLoader");
    assertNotNull(loaded.tabContent(), "tabContent must be injected by FXMLLoader");
  }

  @Test
  void tabButtonsAreCreatedOneForEachVisibleTabInOrder(FxRobot robot) {
    AFFrCalculation cal = calculationWithDefaultModel();

    Loaded loaded = loadEditor(robot, cal);

    assertEquals(
        InputTab.tabsFor(cal.getModel()),
        List.copyOf(loaded.controller().tabButtonsByInputTab().keySet()));
    assertEquals(
        InputTab.tabsFor(cal.getModel()).size(), loaded.tabButtonsBox().getChildren().size());
  }

  /**
   * End-to-end check that the MANAGER honors {@link InputTab#tabsFor} for a non-trivial extra-model
   * combination. The exhaustive matrix is in {@link affr.fx.viewmodel.inputs.InputTabTest}; this
   * test just pins the View↔ViewModel hand-off.
   */
  @Test
  void tabsForCombustChemReactModelIncludeCombustionAndReaction(FxRobot robot) {
    AFFrCalculation cal = calculationWithExtras(ExtraModel.COMBUST_CHEM_REACT);

    Loaded loaded = loadEditor(robot, cal);

    assertEquals(
        List.of(
            InputTab.MESH,
            InputTab.MODEL,
            InputTab.FLUID,
            InputTab.BOUNDARY,
            InputTab.SCHEME,
            InputTab.IO_MONITORING,
            InputTab.COMBUSTION,
            InputTab.REACTION),
        List.copyOf(loaded.controller().tabButtonsByInputTab().keySet()));
  }

  // ── Selection / content swap ──────────────────────────────────────────────

  @Test
  void firstTabIsSelectedByDefaultAndItsPaneIsInTabContent(FxRobot robot) {
    Loaded loaded = loadEditor(robot, calculationWithDefaultModel());

    Map<InputTab, ToggleButton> buttons = loaded.controller().tabButtonsByInputTab();
    ToggleButton first = buttons.values().iterator().next();
    assertTrue(first.isSelected(), "first tab must be selected by default");
    assertEquals(1, loaded.tabContent().getChildren().size(), "tabContent has exactly one child");
  }

  @Test
  void selectingDifferentTabSwapsContent(FxRobot robot) {
    Loaded loaded = loadEditor(robot, calculationWithDefaultModel());
    Map<InputTab, ToggleButton> buttons = loaded.controller().tabButtonsByInputTab();
    List<ToggleButton> ordered = List.copyOf(buttons.values());
    ToggleButton first = ordered.get(0);
    ToggleButton second = ordered.get(1);

    robot.interact(() -> second.setSelected(true));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(second.isSelected());
    assertEquals(false, first.isSelected(), "selecting a new tab must deselect the old one");
    assertEquals(1, loaded.tabContent().getChildren().size(), "previous pane must be removed");
  }

  /**
   * Regression: clicking the already-selected tab must NOT deselect it. Without this guard, the
   * MANAGER could end up with zero active tabs and an empty content pane.
   */
  @Test
  void clickingActiveTabKeepsItSelected(FxRobot robot) {
    Loaded loaded = loadEditor(robot, calculationWithDefaultModel());
    ToggleButton first = loaded.controller().tabButtonsByInputTab().values().iterator().next();

    robot.interact(() -> first.setSelected(false)); // simulate click on the active tab
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(first.isSelected(), "active tab must stay selected after user attempts to toggle");
    assertEquals(1, loaded.tabContent().getChildren().size(), "tabContent must still have a child");
  }

  // ── Locale change ────────────────────────────────────────────────────────

  /**
   * Locale change must re-resolve tab labels via {@link I18n}. The exact Japanese strings are
   * pinned by {@code messages_ja.properties} and asserted in {@link
   * affr.fx.viewmodel.inputs.InputTabI18nTest}; here we only need to prove that the controller's
   * locale-change listener actually fires.
   */
  @Test
  void localeChangeRefreshesTabLabels(FxRobot robot) {
    Loaded loaded = loadEditor(robot, calculationWithDefaultModel());
    ToggleButton meshButton = loaded.controller().tabButtonsByInputTab().get(InputTab.MESH);
    assertNotNull(meshButton, "MESH tab button must exist for the default model");
    String englishMeshText = meshButton.getText();

    robot.interact(() -> I18n.setLocale(Locale.JAPANESE));
    WaitForAsyncUtils.waitForFxEvents();

    assertNotEquals(
        englishMeshText, meshButton.getText(), "tab label must change after locale change");
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private record Loaded(
      InputEditorController controller,
      Parent root,
      Scene scene,
      VBox tabButtonsBox,
      StackPane tabContent) {}

  private Loaded loadEditor(FxRobot robot, AFFrCalculation cal) {
    AtomicReference<Loaded> ref = new AtomicReference<>();
    robot.interact(
        () -> {
          try {
            URL fxml =
                Objects.requireNonNull(
                    InputEditorController.class.getResource("InputEditorController.fxml"),
                    "InputEditorController.fxml not found on classpath");
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            InputEditorController controller = loader.getController();
            controller.init(cal);

            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.show();

            VBox tabButtonsBox =
                Objects.requireNonNull(
                    (VBox) root.lookup("#tabButtons"), "tabButtons not found via lookup");
            StackPane tabContent =
                Objects.requireNonNull(
                    (StackPane) root.lookup("#tabContent"), "tabContent not found via lookup");
            ref.set(new Loaded(controller, root, scene, tabButtonsBox, tabContent));
          } catch (Exception e) {
            throw new RuntimeException("failed to load InputEditorController.fxml", e);
          }
        });
    WaitForAsyncUtils.waitForFxEvents();
    return Objects.requireNonNull(ref.get(), "loadEditor did not produce a Loaded result");
  }

  private static AFFrCalculation calculationWithDefaultModel() {
    return new AFFrCalculation(
        "cal_test",
        PROJ_PATH.resolve("cal_test"),
        null,
        AFFrCalProperty.DEFAULT,
        AFFrCalculationModel.DEFAULT);
  }

  private static AFFrCalculation calculationWithExtras(ExtraModel... extras) {
    AFFrCalculationModel model =
        new AFFrCalculationModel(
            ComprsModel.INCOMPRESSIBLE,
            SteadyModel.STEADY,
            TurbModel.RANS,
            java.util.Set.of(extras));
    return new AFFrCalculation(
        "cal_test", PROJ_PATH.resolve("cal_test"), null, AFFrCalProperty.DEFAULT, model);
  }
}
