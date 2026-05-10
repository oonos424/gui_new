package affr.app.top;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import affr.app.top.file.FileBrowserController;
import affr.data.DataStore;
import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.project.ProjectLoader;
import affr.util.fx.FxScheduler;
import affr.util.prefs.UserPreferences;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * View↔ViewModel integration tests for {@link TopController}.
 *
 * <p>These are the highest-value MVVM tests: they verify that the bindings wired up in {@link
 * TopController#init} faithfully reflect VM state into widgets and user gestures back into the VM,
 * which is the entire contract MVVM exists to enforce.
 *
 * <p>Runs headlessly on Monocle — see {@code build.gradle.kts} {@code tasks.test} for the system
 * properties that select the headless GL pipeline.
 */
@ExtendWith(ApplicationExtension.class)
final class TopControllerTest {

  private TopViewModel viewModel;
  private TopController controller;
  private ListView<TopCategory> categoryList;
  private StackPane viewerPane;
  private BorderPane rootPane;

  @Start
  @SuppressWarnings("unchecked")
  void start(Stage stage) throws Exception {
    // Build the file-browser sub-view (root + controller) — TopController needs both.
    DataStore dataStore = new DataStore(UserPreferences.APP_DIR);
    FileBrowserViewModel fbVm =
        new FileBrowserViewModel(dataStore, new ProjectLoader(), FxScheduler.defaultInstance());
    URL fbFxml =
        Objects.requireNonNull(
            FileBrowserController.class.getResource("FileBrowserController.fxml"),
            "FileBrowserController.fxml not found on classpath");
    FXMLLoader fbLoader = new FXMLLoader(fbFxml);
    Node fbNode = fbLoader.load();
    FileBrowserController fbController = fbLoader.getController();
    fbController.init(fbVm);

    URL fxml =
        Objects.requireNonNull(
            TopController.class.getResource("TopController.fxml"),
            "TopController.fxml not found on classpath");
    FXMLLoader loader = new FXMLLoader(fxml);
    Parent root = loader.load();

    controller = loader.getController();
    viewModel = new TopViewModel();
    controller.init(viewModel, fbVm, fbNode);

    categoryList = (ListView<TopCategory>) root.lookup("#categoryList");
    viewerPane = (StackPane) root.lookup("#viewerPane");
    rootPane = (BorderPane) root;

    stage.setScene(new Scene(root, 800, 600));
    stage.show();
  }

  @Test
  void fxmlInjectsRequiredWidgets() {
    assertNotNull(categoryList, "categoryList must be injected by FXMLLoader");
    assertNotNull(viewerPane, "viewerPane must be injected by FXMLLoader");
  }

  @Test
  void categoryListIsBoundToViewModelCategories() {
    assertSame(viewModel.getCategories(), categoryList.getItems());
    assertEquals(
        List.of(TopCategory.FILE, TopCategory.RUNNING, TopCategory.TUTORIALS),
        categoryList.getItems());
  }

  @Test
  void initialSelectionMirrorsViewModel(FxRobot robot) {
    robot.interact(() -> {});
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(TopCategory.FILE, categoryList.getSelectionModel().getSelectedItem());
    assertSame(TopCategory.FILE, viewModel.getSelectedCategory());
    assertFalse(viewerPane.getChildren().isEmpty(), "viewerPane should contain the file browser");
  }

  @Test
  void changingViewModelSelectionUpdatesListAndViewerPane(FxRobot robot) {
    robot.interact(() -> viewModel.setSelectedCategory(TopCategory.TUTORIALS));
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(TopCategory.TUTORIALS, categoryList.getSelectionModel().getSelectedItem());
    assertEquals(TopCategory.TUTORIALS.label(), labelTextOf(viewerPane));
  }

  @Test
  void changingListSelectionUpdatesViewModel(FxRobot robot) {
    robot.interact(() -> categoryList.getSelectionModel().select(TopCategory.RUNNING));
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(TopCategory.RUNNING, viewModel.getSelectedCategory());
    assertEquals(TopCategory.RUNNING.label(), labelTextOf(viewerPane));
  }

  @Test
  void allCategoriesRoundTripThroughTheView(FxRobot robot) {
    for (TopCategory c : TopCategory.values()) {
      robot.interact(() -> categoryList.getSelectionModel().select(c));
      WaitForAsyncUtils.waitForFxEvents();
      assertSame(c, viewModel.getSelectedCategory());
      assertViewerRendered(c, viewerPane);
    }

    for (TopCategory c : TopCategory.values()) {
      robot.interact(() -> viewModel.setSelectedCategory(c));
      WaitForAsyncUtils.waitForFxEvents();
      assertSame(c, categoryList.getSelectionModel().getSelectedItem());
      assertViewerRendered(c, viewerPane);
    }
  }

  // ── Input Editor mode swap ────────────────────────────────────────────────
  //
  // {@link TopController#enterInputEditorMode(Node)} and {@link
  // TopController#exitInputEditorMode()} together form the layout-swap contract used by
  // NavigationService when the user opens a calculation.

  @Test
  void enterInputEditorModeRemovesTopAndSwapsViewer(FxRobot robot) {
    Node projectNode = new Label("project-view");
    Node editorNode = new Label("editor-view");
    robot.interact(() -> controller.enterProjectMode(projectNode));
    WaitForAsyncUtils.waitForFxEvents();

    robot.interact(() -> controller.enterInputEditorMode(editorNode));
    WaitForAsyncUtils.waitForFxEvents();

    assertNull(rootPane.getTop(), "shell header must be removed in Input Editor mode");
    assertEquals(1, viewerPane.getChildren().size());
    assertSame(editorNode, viewerPane.getChildren().get(0));
  }

  @Test
  void exitInputEditorModeRestoresOriginalTopAndProjectViewer(FxRobot robot) {
    Node projectNode = new Label("project-view");
    Node editorNode = new Label("editor-view");
    robot.interact(() -> controller.enterProjectMode(projectNode));
    WaitForAsyncUtils.waitForFxEvents();
    Node originalTop = rootPane.getTop();
    assertNotNull(originalTop, "shell header must exist before entering Input Editor mode");

    robot.interact(() -> controller.enterInputEditorMode(editorNode));
    WaitForAsyncUtils.waitForFxEvents();
    robot.interact(() -> controller.exitInputEditorMode());
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(originalTop, rootPane.getTop(), "shell header must be restored on exit");
    assertEquals(1, viewerPane.getChildren().size());
    assertSame(projectNode, viewerPane.getChildren().get(0));
  }

  /**
   * Regression for the {@code if (savedTopNode == null)} guard in {@link
   * TopController#enterInputEditorMode(Node)}: a second enter must not overwrite the previously
   * saved header with the (now-null) top region, otherwise exit would leave the header permanently
   * empty.
   */
  @Test
  void inputEditorDoubleEnterDoesNotLoseSavedTop(FxRobot robot) {
    Node projectNode = new Label("project-view");
    Node firstEditor = new Label("editor-1");
    Node secondEditor = new Label("editor-2");
    robot.interact(() -> controller.enterProjectMode(projectNode));
    WaitForAsyncUtils.waitForFxEvents();
    Node originalTop = rootPane.getTop();

    robot.interact(() -> controller.enterInputEditorMode(firstEditor));
    WaitForAsyncUtils.waitForFxEvents();
    robot.interact(() -> controller.enterInputEditorMode(secondEditor));
    WaitForAsyncUtils.waitForFxEvents();
    robot.interact(() -> controller.exitInputEditorMode());
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(originalTop, rootPane.getTop(), "double-enter must not lose the saved top node");
  }

  @Test
  void exitInputEditorModeWithoutEnterIsNoOp(FxRobot robot) {
    Node originalTop = rootPane.getTop();

    robot.interact(() -> controller.exitInputEditorMode());
    WaitForAsyncUtils.waitForFxEvents();

    assertSame(originalTop, rootPane.getTop(), "exit must not mutate top when never entered");
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  /**
   * Asserts that the viewer pane contains the correct content for {@code category}.
   *
   * <p>FILE renders the file-browser sub-view (no placeholder label). RUNNING and TUTORIALS render
   * a placeholder {@link Label} with the category's i18n label text.
   */
  private static void assertViewerRendered(TopCategory category, StackPane pane) {
    if (category == TopCategory.FILE) {
      assertFalse(pane.getChildren().isEmpty(), "viewerPane should contain the file browser");
    } else {
      assertEquals(category.label(), labelTextOf(pane));
    }
  }

  /**
   * Reads the text of the placeholder {@link Label} the controller renders into the viewer pane.
   * Throws {@link AssertionError} if no label is present.
   */
  private static String labelTextOf(StackPane pane) {
    Label label =
        pane.getChildren().stream()
            .filter(Label.class::isInstance)
            .map(Label.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("viewerPane has no Label placeholder"));
    return Objects.requireNonNull(label.getText(), "placeholder Label text is null");
  }
}
