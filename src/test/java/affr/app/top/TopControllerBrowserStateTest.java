package affr.app.top;

import static org.junit.jupiter.api.Assertions.assertEquals;

import affr.data.DataStore;
import affr.fx.viewmodel.top.TopViewModel;
import affr.util.prefs.UserPreferences;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Integration tests that verify the browser-state persistence wiring added to {@link
 * TopController}.
 *
 * <p>Each test uses a {@link TempDir}-backed {@link UserPreferences} so assertions can inspect
 * in-memory pref values without touching {@code ~/.affr/}.
 *
 * <p>Runs headlessly on Monocle — see {@code build.gradle.kts} for the system properties.
 */
@ExtendWith(ApplicationExtension.class)
final class TopControllerBrowserStateTest {

  // JUnit populates @TempDir fields before ApplicationExtension calls @Start,
  // so the temp directory is available when the stage is built.
  @TempDir Path tempDir;

  private UserPreferences prefs;

  @Start
  void start(Stage stage) throws Exception {
    prefs = UserPreferences.loadFrom(tempDir.resolve("prefs.properties"));

    URL fxml =
        Objects.requireNonNull(
            TopController.class.getResource("TopController.fxml"),
            "TopController.fxml not found on classpath");
    FXMLLoader loader = new FXMLLoader(fxml);
    Parent root = loader.load();

    TopController controller = loader.getController();
    DataStore dataStore = new DataStore(tempDir);
    controller.init(new TopViewModel(), prefs, dataStore);

    stage.setScene(new Scene(root, 800, 600));
    stage.show();
  }

  // ── Persistence tests (view-mode toggle → prefs) ───────────────────────────

  @Test
  void switchingToTreeModePersistsViewModeInPrefs(FxRobot robot) {
    WaitForAsyncUtils.waitForFxEvents();

    robot.clickOn("#treeModeButton");
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("TREE", prefs.browserViewMode());
  }

  @Test
  void switchingToIconModePersistsViewModeInPrefs(FxRobot robot) {
    WaitForAsyncUtils.waitForFxEvents();

    robot.clickOn("#iconModeButton");
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("ICON", prefs.browserViewMode());
  }

  @Test
  void switchingBackToListModePersistsViewModeInPrefs(FxRobot robot) {
    WaitForAsyncUtils.waitForFxEvents();

    robot.clickOn("#treeModeButton");
    robot.clickOn("#listModeButton");
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("LIST", prefs.browserViewMode());
  }
}
