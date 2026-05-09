package affr.app;

import affr.app.top.TopController;
import affr.data.DataStore;
import affr.fx.viewmodel.top.TopViewModel;
import affr.util.prefs.UserPreferences;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Loads screens, constructs their ViewModels, and transitions the primary {@link Stage} between
 * them.
 *
 * <p>This is the only place that simultaneously knows about the View layer (FXML, controllers) and
 * the ViewModel layer (creating ViewModels, wiring application services into them). Controllers
 * never see {@link AppConfig} or this service directly; ViewModels see only what they need.
 */
public final class NavigationService {

  private final Stage stage;
  private final AppConfig config;
  private final UserPreferences prefs;

  public NavigationService(Stage stage, AppConfig config, UserPreferences prefs) {
    this.stage = stage;
    this.config = config;
    this.prefs = prefs;
  }

  /** Loads and displays the Project Browser (first screen). */
  public void showProjectBrowser() throws IOException {
    TopViewModel viewModel = new TopViewModel();
    DataStore dataStore = new DataStore(UserPreferences.APP_DIR);

    URL fxml = requireResource(TopController.class, "TopController.fxml");
    FXMLLoader loader = new FXMLLoader(fxml);
    Parent root = loader.load();
    if (root == null) {
      throw new IllegalStateException("Loaded TopController root is null");
    }
    TopController controller = loader.getController();
    if (controller == null) {
      throw new IllegalStateException("TopController was not set by FXMLLoader");
    }
    controller.init(viewModel, prefs, dataStore);

    Scene scene = new Scene(root, prefs.windowWidth(), prefs.windowHeight());
    stage.setTitle("AFFr");
    stage.setScene(scene);

    // Save window bounds when the user closes the window.
    stage.setOnHiding(
        e -> {
          double w = stage.getWidth();
          double h = stage.getHeight();
          if (Double.isFinite(w) && w > 0 && Double.isFinite(h) && h > 0) {
            prefs.setWindowSize(w, h);
            prefs.setWindowPosition(stage.getX(), stage.getY());
            prefs.save();
          }
        });

    stage.show();

    // Restore saved position after show() — applying it before show() is overridden by
    // the OS window manager on macOS. Skip if not yet saved or outside all active screens.
    if (isOnScreen(prefs.windowX(), prefs.windowY())) {
      stage.setX(prefs.windowX());
      stage.setY(prefs.windowY());
    }
  }

  public AppConfig config() {
    return config;
  }

  /**
   * Returns {@code true} if the point ({@code x}, {@code y}) falls within the visual bounds of at
   * least one currently connected screen.
   *
   * <p>Used to guard against restoring a window position that was saved on a monitor that is no
   * longer available, which would otherwise leave the window inaccessible off-screen.
   */
  private static boolean isOnScreen(double x, double y) {
    if (!Double.isFinite(x) || !Double.isFinite(y)) return false;
    for (Screen screen : Screen.getScreens()) {
      Rectangle2D bounds = screen.getVisualBounds();
      if (bounds.contains(x, y)) return true;
    }
    return false;
  }

  private static URL requireResource(Class<?> owner, String resourceName) {
    URL url = owner.getResource(resourceName);
    if (url == null) {
      throw new IllegalStateException(resourceName + " not found on classpath");
    }
    return url;
  }
}
