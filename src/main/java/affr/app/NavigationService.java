package affr.app;

import affr.app.top.TopController;
import affr.fx.viewmodel.top.TopViewModel;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

  private static final double INITIAL_WIDTH = 1200;
  private static final double INITIAL_HEIGHT = 720;

  private final Stage stage;
  private final AppConfig config;

  public NavigationService(Stage stage, AppConfig config) {
    this.stage = stage;
    this.config = config;
  }

  /** Loads and displays the Project Browser (first screen). */
  public void showProjectBrowser() throws IOException {
    TopViewModel viewModel = new TopViewModel();

    URL fxml =
        Objects.requireNonNull(
            TopController.class.getResource("TopController.fxml"),
            "TopController.fxml not found on classpath");
    FXMLLoader loader = new FXMLLoader(fxml);
    Parent root = loader.load();
    TopController controller = loader.getController();
    controller.init(viewModel);

    Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
    stage.setTitle("AFFr");
    stage.setScene(scene);
    stage.show();
  }

  public AppConfig config() {
    return config;
  }
}
