package affr.app.top;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.data.DataStore;
import affr.fx.viewmodel.top.TopViewModel;
import affr.util.prefs.UserPreferences;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Integration tests that verify the browser-state restore wiring added to {@link TopController}.
 *
 * <p>Each test pre-populates the preferences file before the controller is initialised so that
 * {@code restoreBrowserState} has something to act on, then asserts the resulting widget state.
 *
 * <p>Runs headlessly on Monocle — see {@code build.gradle.kts} for the system properties.
 */
@ExtendWith(ApplicationExtension.class)
final class TopControllerBrowserStateRestoreTest {

  @TempDir Path tempDir;

  @Start
  void start(Stage stage) throws Exception {
    // Write a prefs file that records TREE as the last-used view mode.
    Path prefsFile = tempDir.resolve("prefs.properties");
    Files.writeString(prefsFile, "browser.viewMode=TREE\n");

    UserPreferences prefs = UserPreferences.loadFrom(prefsFile);

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

  // ── Restore tests (saved prefs → correct widget state on startup) ──────────

  @Test
  void savedTreeModeIsRestoredOnStartup(FxRobot robot) {
    WaitForAsyncUtils.waitForFxEvents();

    ToggleButton treeBtn = robot.lookup("#treeModeButton").queryAs(ToggleButton.class);
    ToggleButton listBtn = robot.lookup("#listModeButton").queryAs(ToggleButton.class);

    assertTrue(treeBtn.isSelected(), "treeModeButton should be selected after restore");
    assertFalse(listBtn.isSelected(), "listModeButton should not be selected after restore");
  }

  @Test
  void savedTreeModeIsRestoredAndIconModeIsNotSelected(FxRobot robot) {
    WaitForAsyncUtils.waitForFxEvents();

    ToggleButton iconBtn = robot.lookup("#iconModeButton").queryAs(ToggleButton.class);
    assertFalse(iconBtn.isSelected(), "iconModeButton should not be selected after restore");
  }
}
