package affr.app;

import affr.app.inputs.InputEditorController;
import affr.app.top.TopController;
import affr.app.top.file.FileBrowserController;
import affr.app.top.file.ProjectController;
import affr.data.DataStore;
import affr.fx.viewmodel.top.TopViewModel;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.fx.viewmodel.top.file.ProjectViewModel;
import affr.project.AFFrCalculation;
import affr.project.AFFrProject;
import affr.project.ProjectLoader;
import affr.util.fx.FxScheduler;
import affr.util.prefs.UserPreferences;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Loads screens, constructs ViewModels, wires their cross-screen interactions, and transitions the
 * primary {@link Stage} between them.
 *
 * <p>This is the only place that simultaneously knows about the View layer (FXML, controllers) and
 * the ViewModel layer (creating ViewModels, injecting application services into them). Controllers
 * never see {@link AppConfig}, {@link UserPreferences}, or this service directly; ViewModels see
 * only the services they need ({@link DataStore}, {@link ProjectLoader}, {@link FxScheduler}).
 *
 * <p>Persistence wiring (window bounds, locale, browser path/mode) is delegated to {@link
 * PreferencesAdapter}, constructed once after the ViewModels exist.
 */
public final class NavigationService {

  private final Stage stage;
  private final AppConfig config;
  private final UserPreferences prefs;
  private final FxScheduler scheduler = FxScheduler.defaultInstance();

  // Set when showProjectBrowser() runs; cleared on hide.
  private @Nullable TopController topController;
  private @Nullable FileBrowserViewModel fileBrowserViewModel;
  private @Nullable FileBrowserViewModel tutorialBrowserViewModel;

  public NavigationService(Stage stage, AppConfig config, UserPreferences prefs) {
    this.stage = stage;
    this.config = config;
    this.prefs = prefs;
  }

  /** Loads and displays the Project Browser (first screen). */
  public void showProjectBrowser() throws IOException {
    DataStore dataStore = new DataStore(UserPreferences.APP_DIR);
    ProjectLoader projectLoader = new ProjectLoader();

    TopViewModel topVm = new TopViewModel();
    FileBrowserViewModel fbVm = new FileBrowserViewModel(dataStore, projectLoader, scheduler);

    PreferencesAdapter prefsAdapter = new PreferencesAdapter(prefs, stage, fbVm);
    prefsAdapter.restoreInto(fbVm, dataStore);
    prefsAdapter.install();

    Node fileBrowserNode = loadFileBrowser(fbVm);

    // ── Tutorial browser ─────────────────────────────────────────────────
    Path resolvedTutorialDir = resolveTutorialDir();
    FileBrowserViewModel tutorialFbVm = null;
    Node tutorialBrowserNode = null;
    if (resolvedTutorialDir != null) {
      DataStore tutorialStore = new DataStore(resolvedTutorialDir, true);
      tutorialFbVm = new FileBrowserViewModel(tutorialStore, projectLoader, scheduler);
      tutorialFbVm.setReadOnlyRoot(true);
      tutorialFbVm.setTutorialMirrorRoot(UserPreferences.APP_DIR.resolve(".tutorials"));
      tutorialBrowserNode = loadFileBrowser(tutorialFbVm);
    }

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
    controller.init(topVm, fbVm, fileBrowserNode, tutorialFbVm, tutorialBrowserNode);
    controller.setOnSettingAction(() -> SettingsDialog.show(stage, prefs));

    this.topController = controller;
    this.fileBrowserViewModel = fbVm;
    this.tutorialBrowserViewModel = tutorialFbVm;

    // Subscribe to project loads completed by FileBrowserViewModel and route to project screen.
    fbVm.openedProjectProperty()
        .addListener(
            (obs, old, project) -> {
              if (project != null) {
                showProject(project);
              }
            });

    // Subscribe to project loads from the tutorial browser as well.
    if (tutorialFbVm != null) {
      tutorialFbVm
          .openedProjectProperty()
          .addListener(
              (obs, old, project) -> {
                if (project != null) {
                  showProject(project);
                }
              });
    }

    Scene scene = new Scene(root, prefs.windowWidth(), prefs.windowHeight());
    stage.setTitle("AFFr");
    stage.setScene(scene);

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

  // ── Sub-view loading ─────────────────────────────────────────────────────

  private Node loadFileBrowser(FileBrowserViewModel fbVm) throws IOException {
    URL fxml = requireResource(FileBrowserController.class, "FileBrowserController.fxml");
    FXMLLoader loader = new FXMLLoader(fxml);
    Node node = loader.load();
    if (node == null) {
      throw new IllegalStateException("Loaded FileBrowserController root is null");
    }
    FileBrowserController controller = loader.getController();
    if (controller == null) {
      throw new IllegalStateException("FileBrowserController was not set by FXMLLoader");
    }
    controller.init(fbVm);
    return node;
  }

  /**
   * Loads and displays the {@link ProjectController} for the given project. Delegates the layout
   * swap to {@link TopController#enterProjectMode(Node)}.
   */
  private void showProject(AFFrProject project) {
    ProjectViewModel pvm = new ProjectViewModel(project);
    URL fxml = requireResource(ProjectController.class, "ProjectController.fxml");
    FXMLLoader loader = new FXMLLoader(fxml);
    Node node;
    try {
      node = loader.load();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load ProjectController.fxml", e);
    }
    if (node == null) {
      throw new IllegalStateException("FXMLLoader returned null node for ProjectController.fxml");
    }
    ProjectController controller = loader.getController();
    if (controller == null) {
      throw new IllegalStateException("ProjectController was not set by FXMLLoader");
    }
    controller.init(pvm);

    // Subscribe to open-calculation requests from this project so double-clicks route
    // into the Input Editor.
    pvm.openCalculationRequestProperty()
        .addListener(
            (obs, old, calculation) -> {
              if (calculation != null) {
                showInputEditor(calculation);
                pvm.clearOpenCalculationRequest();
              }
            });

    requireTopController().enterProjectMode(node);
    // Acknowledge the openedProject signal so the same project can be re-opened.
    // Clear both VMs: only one triggered the open, but clearing the other is a no-op.
    requireFileBrowserViewModel().clearOpenedProject();
    requireFileBrowserViewModel().setOpeningProject(null);
    FileBrowserViewModel tutVm = tutorialBrowserViewModel;
    if (tutVm != null) {
      tutVm.clearOpenedProject();
      tutVm.setOpeningProject(null);
    }
  }

  /**
   * Resolves the tutorial inventory directory from user preferences: returns {@code
   * guiInstallPath().resolve("tutorials")} when the GUI installation path is configured, or {@code
   * null} when it is not.
   *
   * <p>The returned path may or may not exist on disk; callers must check.
   */
  private @Nullable Path resolveTutorialDir() {
    Path installPath = prefs.guiInstallPath();
    if (installPath != null) {
      return installPath.resolve("tutorials");
    }
    return null;
  }

  /**
   * Loads the Input Editor for the given calculation and opens it in a new workspace tab.
   *
   * <p>Back (inside the editor) closes the tab and returns to the project's calculation list (the
   * primary tab remains in project mode). Home closes the tab and also exits project mode,
   * returning the primary tab to the Project Browser.
   */
  private void showInputEditor(AFFrCalculation calculation) {
    URL fxml = requireResource(InputEditorController.class, "InputEditorController.fxml");
    FXMLLoader loader = new FXMLLoader(fxml);
    Node node;
    try {
      node = loader.load();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load InputEditorController.fxml", e);
    }
    if (node == null) {
      throw new IllegalStateException(
          "FXMLLoader returned null node for InputEditorController.fxml");
    }
    InputEditorController controller = loader.getController();
    if (controller == null) {
      throw new IllegalStateException("InputEditorController was not set by FXMLLoader");
    }

    TopController top = requireTopController();
    Runnable closeThisTab = () -> top.closeTab(top.indexOfNode(node));
    controller.init(
        calculation,
        closeThisTab, // Back = close tab only
        () -> {
          closeThisTab.run();
          top.exitProjectMode();
        }); // Home = close tab + exit project

    top.openTab(calculation.name(), node, closeThisTab);
  }

  private TopController requireTopController() {
    TopController c = topController;
    if (c == null) throw new IllegalStateException("showProjectBrowser() has not been called");
    return c;
  }

  private FileBrowserViewModel requireFileBrowserViewModel() {
    FileBrowserViewModel vm = fileBrowserViewModel;
    if (vm == null) throw new IllegalStateException("showProjectBrowser() has not been called");
    return vm;
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
