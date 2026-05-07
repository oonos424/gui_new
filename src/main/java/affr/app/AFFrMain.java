package affr.app;

import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
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
    UserPreferences prefs = UserPreferences.load();
    I18n.setLocale(prefs.locale());
    NavigationService nav = new NavigationService(stage, config, prefs);
    nav.showProjectBrowser();
  }
}
