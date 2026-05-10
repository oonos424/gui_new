package affr.app.top.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.fx.viewmodel.top.file.ProjectViewModel;
import affr.project.AFFrCalProperty;
import affr.project.AFFrCalculation;
import affr.project.AFFrCalculationModel;
import affr.project.AFFrProject;
import affr.project.ProjectItem;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * View↔ViewModel integration tests for {@link ProjectController}.
 *
 * <p>Scope: focused on the post-create wiring exposed via {@link
 * ProjectController#onCalculationCreated(AFFrCalculation)} — the existing creation flow's success
 * handler — which is the single place where the auto-navigate-to-Input-Editor behavior is wired.
 *
 * <p>The async {@code Task} that drives {@code ProjectWriter.createCalculation} on disk is not
 * exercised here; that's covered by {@link affr.project.ProjectWriterTest}. Splitting the success
 * handler out of the lambda gives this test a stable seam without introducing virtual-thread waits.
 *
 * <p>Runs headlessly on Monocle — see {@code build.gradle.kts} {@code tasks.test}.
 */
@ExtendWith(ApplicationExtension.class)
final class ProjectControllerTest {

  private static final Path PROJ_PATH = Path.of("/tmp/project_controller_test_project");

  private ProjectController controller;
  private ProjectViewModel viewModel;

  @SuppressWarnings("unchecked")
  private ListView<ProjectItem> itemList;

  @Start
  @SuppressWarnings("unchecked")
  void start(Stage stage) throws Exception {
    URL fxml =
        Objects.requireNonNull(
            ProjectController.class.getResource("ProjectController.fxml"),
            "ProjectController.fxml not found on classpath");
    FXMLLoader loader = new FXMLLoader(fxml);
    Parent root = loader.load();

    controller = loader.getController();
    AFFrProject project = new AFFrProject("p", PROJ_PATH, "", List.of());
    viewModel = new ProjectViewModel(project);
    controller.init(viewModel);

    itemList = (ListView<ProjectItem>) root.lookup("#itemList");

    stage.setScene(new Scene(root, 800, 600));
    stage.show();
  }

  // ── onCalculationCreated ──────────────────────────────────────────────────

  @Test
  void onCalculationCreatedAddsCalculationToViewModel(FxRobot robot) {
    AFFrCalculation cal = makeCal("cal_01");

    robot.interact(() -> controller.onCalculationCreated(cal));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(1, viewModel.getProjectItems().size());
    assertSame(cal, viewModel.getProjectItems().get(0));
  }

  @Test
  void onCalculationCreatedSelectsTheNewCalculationInTheList(FxRobot robot) {
    AFFrCalculation cal = makeCal("cal_01");

    robot.interact(() -> controller.onCalculationCreated(cal));
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(cal, itemList.getSelectionModel().getSelectedItem());
  }

  /**
   * Regression for the auto-navigate-to-Input-Editor wiring. {@link
   * ProjectController#onCalculationCreated} must fire {@code openCalculationRequest} so {@link
   * affr.app.NavigationService} opens the Input Editor for the just-created calculation.
   */
  @Test
  void onCalculationCreatedFiresOpenCalculationRequest(FxRobot robot) {
    AFFrCalculation cal = makeCal("cal_01");
    assertNull(viewModel.openCalculationRequestProperty().get(), "precondition: request is null");

    robot.interact(() -> controller.onCalculationCreated(cal));
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(cal, viewModel.openCalculationRequestProperty().get());
  }

  /**
   * Two consecutive creations must each end up with their own open-request signal. Without the
   * {@code clearOpenCalculationRequest} call inside the navigation listener, the second {@code
   * requestOpenCalculation(cal2)} would still fire (different value), but this exercises the
   * controller's contract that each creation is independently observable.
   */
  @Test
  void onCalculationCreatedTwiceLeavesTheSecondCalculationAsTheLatestRequest(FxRobot robot) {
    AFFrCalculation first = makeCal("cal_01");
    AFFrCalculation second = makeCal("cal_02");

    robot.interact(() -> controller.onCalculationCreated(first));
    WaitForAsyncUtils.waitForFxEvents();
    // Simulate the navigation layer's ack-clear that happens after it opens the editor.
    robot.interact(() -> viewModel.clearOpenCalculationRequest());
    WaitForAsyncUtils.waitForFxEvents();

    robot.interact(() -> controller.onCalculationCreated(second));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(2, viewModel.getProjectItems().size());
    assertSame(second, viewModel.openCalculationRequestProperty().get());
    assertSame(second, itemList.getSelectionModel().getSelectedItem());
  }

  @Test
  void onCalculationCreatedMakesSelectionVisibleToFocusedItemBinding(FxRobot robot) {
    AFFrCalculation cal = makeCal("cal_01");

    robot.interact(() -> controller.onCalculationCreated(cal));
    WaitForAsyncUtils.waitForFxEvents();

    // The init() listener mirrors list selection back into the VM's focusedItem property.
    assertSame(cal, viewModel.getFocusedItem());
    assertTrue(
        viewModel.getSortedItems().contains(cal),
        "the new calculation must appear in the sorted view used by the ListView");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static AFFrCalculation makeCal(String name) {
    return new AFFrCalculation(
        name, PROJ_PATH.resolve(name), null, AFFrCalProperty.DEFAULT, AFFrCalculationModel.DEFAULT);
  }
}
