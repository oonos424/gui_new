package affr.app.top.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.project.AFFrCalculationModel;
import affr.project.ComprsModel;
import affr.project.SteadyModel;
import affr.project.TurbModel;
import affr.util.i18n.I18n;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * View↔ViewModel integration tests for {@link NewCalculationDialogController}.
 *
 * <p>Covers the public API the calling controller relies on: name-field seeding via {@link
 * NewCalculationDialogController#setDefaultName(String)}, trimmed read-back via {@link
 * NewCalculationDialogController#getName()}, observability via {@link
 * NewCalculationDialogController#nameProperty()}, and that {@link
 * NewCalculationDialogController#buildResult()} reflects the default combo-box state.
 *
 * <p>Runs headlessly on Monocle — see {@code build.gradle.kts} {@code tasks.test}.
 */
@ExtendWith(ApplicationExtension.class)
final class NewCalculationDialogControllerTest {

  private Stage stage;

  @Start
  void start(Stage stage) {
    this.stage = stage;
  }

  @AfterEach
  void resetLocale(FxRobot robot) {
    robot.interact(() -> I18n.setLocale(Locale.ENGLISH));
  }

  // ── Name field ────────────────────────────────────────────────────────────

  @Test
  void fxmlInjectsNameField(FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);

    assertNotNull(loaded.nameField(), "nameField must be injected by FXMLLoader");
  }

  @Test
  void setDefaultNamePopulatesTheField(FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);

    robot.interact(() -> loaded.controller().setDefaultName("cal_07"));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("cal_07", loaded.nameField().getText());
    assertEquals("cal_07", loaded.controller().getName());
  }

  @Test
  void getNameTrimsLeadingAndTrailingWhitespace(FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);

    robot.interact(() -> loaded.nameField().setText("  spaced  "));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("spaced", loaded.controller().getName());
  }

  @Test
  void getNameReturnsEmptyStringForBlankInput(FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);

    robot.interact(() -> loaded.nameField().setText("    "));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("", loaded.controller().getName());
    assertTrue(loaded.controller().getName().isEmpty(), "blank input must read back as empty");
  }

  @Test
  void namePropertyNotifiesListenersOnEdit(FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);
    AtomicInteger calls = new AtomicInteger();
    AtomicReference<@org.checkerframework.checker.nullness.qual.Nullable String> latest =
        new AtomicReference<>();
    robot.interact(
        () ->
            loaded
                .controller()
                .nameProperty()
                .addListener(
                    (obs, old, n) -> {
                      calls.incrementAndGet();
                      latest.set(n);
                    }));

    robot.interact(() -> loaded.nameField().setText("cal_42"));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(1, calls.get(), "nameProperty must fire exactly one notification per edit");
    assertEquals("cal_42", latest.get());
  }

  @Test
  void namePropertyIsTheSameInstanceAsTheFieldsTextProperty(
      FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);

    assertSame(
        loaded.nameField().textProperty(),
        loaded.controller().nameProperty(),
        "nameProperty must directly expose the field's textProperty");
  }

  // ── Default state of buildResult ──────────────────────────────────────────

  /**
   * Smoke check that {@link NewCalculationDialogController#buildResult()} reflects the default
   * combo-box state set by {@link NewCalculationDialogController#initialize()}: incompressible /
   * steady / RANS / no extras. Exhaustive coverage of the constraint engine lives in {@code
   * ModelConstraintsTest}.
   */
  @Test
  void buildResultReturnsDefaultModelOnFreshDialog(FxRobot robot, @TempDir Path projectDir) {
    Loaded loaded = loadDialog(robot, projectDir);

    AtomicReference<AFFrCalculationModel> result = new AtomicReference<>();
    robot.interact(() -> result.set(loaded.controller().buildResult()));

    AFFrCalculationModel model = result.get();
    assertNotNull(model);
    assertEquals(ComprsModel.INCOMPRESSIBLE, model.comprsModel());
    assertEquals(SteadyModel.STEADY, model.steadyModel());
    assertEquals(TurbModel.RANS, model.turbModel());
    assertTrue(model.extraModelSet().isEmpty(), "default extras must be empty");
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private record Loaded(
      NewCalculationDialogController controller, DialogPane dialogPane, TextField nameField) {}

  private Loaded loadDialog(FxRobot robot, Path projectDir) {
    AtomicReference<Loaded> ref = new AtomicReference<>();
    robot.interact(
        () -> {
          try {
            URL fxml =
                Objects.requireNonNull(
                    NewCalculationDialogController.class.getResource("NewCalculationDialog.fxml"),
                    "NewCalculationDialog.fxml not found on classpath");
            FXMLLoader loader = new FXMLLoader(fxml);
            DialogPane dialogPane = loader.load();
            NewCalculationDialogController controller = loader.getController();
            controller.setProjectPath(projectDir);

            stage.setScene(new Scene(dialogPane, 700, 500));
            stage.show();

            TextField nameField =
                Objects.requireNonNull(
                    (TextField) dialogPane.lookup("#nameField"), "nameField not found via lookup");
            ref.set(new Loaded(controller, dialogPane, nameField));
          } catch (Exception e) {
            throw new RuntimeException("failed to load NewCalculationDialog.fxml", e);
          }
        });
    WaitForAsyncUtils.waitForFxEvents();
    return Objects.requireNonNull(ref.get(), "loadDialog did not produce a Loaded result");
  }
}
