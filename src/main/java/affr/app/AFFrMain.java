package affr.app;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point. Parses CLI arguments and delegates startup to {@link NavigationService}.
 */
public final class AFFrMain extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    AppConfig config = AppConfig.parse(getParameters().getRaw());
    NavigationService nav = new NavigationService(stage, config);
    nav.showProjectBrowser();
  }
}
