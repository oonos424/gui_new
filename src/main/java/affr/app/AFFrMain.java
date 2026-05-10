package affr.app;

import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
import java.lang.invoke.MethodHandles;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Application entry point. Parses CLI arguments and delegates startup to {@link NavigationService}.
 */
public final class AFFrMain extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    LOG.info("AFFr GUI starting");

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
