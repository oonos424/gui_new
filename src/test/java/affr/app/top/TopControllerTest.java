package affr.app.top;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import affr.data.DataStore;
import affr.util.prefs.UserPreferences;
import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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

  @Start
  @SuppressWarnings("unchecked")
  void start(Stage stage) throws Exception {
    URL fxml =
        Objects.requireNonNull(
            TopController.class.getResource("TopController.fxml"),
            "TopController.fxml not found on classpath");
    FXMLLoader loader = new FXMLLoader(fxml);
    Parent root = loader.load();

    controller = loader.getController();
    viewModel = new TopViewModel();
    DataStore dataStore = new DataStore(UserPreferences.APP_DIR);
    controller.init(viewModel, UserPreferences.load(), dataStore);

    categoryList = (ListView<TopCategory>) root.lookup("#categoryList");
    viewerPane = (StackPane) root.lookup("#viewerPane");

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
    // FILE now shows the real file browser — viewerPane has a non-label child.
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
    // Walk every category in both directions to catch any binding that silently breaks.
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

  // ── Helpers ────────────────────────────────────────────────────────────────

  /**
   * Asserts that the viewer pane contains the correct content for {@code category}.
   *
   * <p>FILE renders the real file-browser sub-view (no placeholder label). RUNNING and TUTORIALS
   * still render a placeholder {@link Label} with the category's i18n label text.
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
