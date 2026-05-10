package affr.app.top;

import affr.app.top.file.FileBrowserController;
import affr.app.top.file.ProjectController;
import affr.data.DataStore;
import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import affr.fx.viewmodel.top.file.FileBrowserViewMode;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.fx.viewmodel.top.file.ProjectViewModel;
import affr.project.AFFrProject;
import affr.project.ProjectLoader;
import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * View controller for the Project Browser screen.
 *
 * <p>This class owns only widgets and the bindings that connect them to its {@link TopViewModel}.
 * It contains no domain data and no application services — those live in (or behind) the ViewModel.
 *
 * <p>Lifecycle: FXML loading injects the widgets, then {@link #initialize()} runs (rendering rules
 * only), then the application calls {@link #init(TopViewModel, UserPreferences, DataStore)} to wire
 * bindings.
 *
 * <p>The header navigation area ({@code headerNav}) is shared chrome owned by this controller. It
 * is shown and wired to the {@link FileBrowserViewModel} when the FILE category is active, and
 * hidden for all other categories.
 */
public final class TopController {

  // -------------------------------------------------------------------------
  // FXML-injected widgets
  // -------------------------------------------------------------------------

  @FXML private @Nullable BorderPane rootPane;
  @FXML private @Nullable ListView<TopCategory> categoryList;
  @FXML private @Nullable StackPane viewerPane;
  @FXML private @Nullable RadioMenuItem langEnItem;
  @FXML private @Nullable RadioMenuItem langJaItem;

  // Header navigation controls (File-browser breadcrumb)
  @FXML private @Nullable HBox headerNav;
  @FXML private @Nullable Button headerNavUpButton;
  @FXML private @Nullable Label headerNavPathLabel;

  // Header back / home navigation (shown when a project is open)
  @FXML private @Nullable HBox headerBackNav;
  @FXML private @Nullable Button headerBackButton;
  @FXML private @Nullable Button headerHomeButton;
  @FXML private @Nullable Label appTitleLabel;

  // -------------------------------------------------------------------------
  // ViewModel + services (set by init())
  // -------------------------------------------------------------------------

  private @Nullable TopViewModel viewModel;
  private @Nullable DataStore dataStore;
  private @Nullable UserPreferences prefs;

  // -------------------------------------------------------------------------
  // Lazily-created sub-view state (cached after first creation)
  // -------------------------------------------------------------------------

  private @Nullable Node fileBrowserNode;
  private @Nullable FileBrowserController fileBrowserController;
  private @Nullable FileBrowserViewModel fileBrowserViewModel;

  // -------------------------------------------------------------------------
  // FXML lifecycle — rendering rules only, no data
  // -------------------------------------------------------------------------

  @FXML
  private void initialize() {
    requireCategoryList()
        .setCellFactory(
            lv ->
                new ListCell<>() {
                  {
                    hoverProperty()
                        .addListener(
                            (obs, old, hovered) -> {
                              if (!isSelected()) {
                                setStyle(hovered ? "-fx-background-color: #eaf4ff;" : "");
                              }
                            });
                    selectedProperty()
                        .addListener(
                            (obs, old, selected) -> {
                              if (selected || !isHover()) {
                                setStyle("");
                              }
                            });
                  }

                  @Override
                  protected void updateItem(@Nullable TopCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item != null && !empty ? I18n.get(item.messageKey()) : null);
                  }
                });
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Wires all bindings between widgets and the ViewModel, and sets up the language-selector menu.
   *
   * @param viewModel the ViewModel for this screen
   * @param prefs user preferences used to persist the selected language
   * @param dataStore the master data store used to back the FILE-category view
   */
  public void init(TopViewModel viewModel, UserPreferences prefs, DataStore dataStore) {
    this.viewModel = viewModel;
    this.dataStore = dataStore;
    this.prefs = prefs;

    ListView<TopCategory> list = requireCategoryList();
    list.setItems(viewModel.getCategories());

    // ── ListView selection → ViewModel ──────────────────────────────────
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, selected) -> {
              if (selected != null) {
                viewModel.setSelectedCategory(selected);
              }
            });

    // ── ViewModel selection → ListView + viewer area ─────────────────
    viewModel
        .selectedCategoryProperty()
        .addListener(
            (obs, old, selected) -> {
              list.getSelectionModel().select(selected);
              renderViewer(selected);
            });

    // Initial sync
    list.getSelectionModel().select(viewModel.getSelectedCategory());
    renderViewer(viewModel.getSelectedCategory());

    // ── Language menu ────────────────────────────────────────────────
    syncLanguageMenu(I18n.getLocale());

    requireLangEnItem()
        .setOnAction(
            e -> {
              I18n.setLocale(Locale.ENGLISH);
              prefs.setLocale(Locale.ENGLISH);
              prefs.save();
            });

    requireLangJaItem()
        .setOnAction(
            e -> {
              I18n.setLocale(Locale.JAPANESE);
              prefs.setLocale(Locale.JAPANESE);
              prefs.save();
            });

    // ── Refresh widgets when locale changes ──────────────────────────
    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              requireCategoryList().refresh();
              renderViewer(requireViewModel().getSelectedCategory());
            });

    // ── Back / Home navigation ───────────────────────────────────────
    requireHeaderBackButton().setOnAction(e -> showBrowser());
    requireHeaderHomeButton().setOnAction(e -> showBrowser());
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Renders the viewer pane and updates the header navigation area for the given category.
   *
   * <p>FILE: shows the real file-browser node and the header nav controls (Up + breadcrumb).
   * RUNNING / TUTORIALS: shows a placeholder label and hides the header nav.
   */
  private void renderViewer(TopCategory category) {
    StackPane pane = requireViewerPane();
    switch (category) {
      case FILE -> {
        requireHeaderNav().setVisible(true);
        pane.getChildren().setAll(requireFileBrowserNode());
      }
      case RUNNING, TUTORIALS -> {
        requireHeaderNav().setVisible(false);
        Label placeholder = new Label(I18n.get(category.messageKey()));
        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #888;");
        pane.getChildren().setAll(placeholder);
      }
    }
  }

  /**
   * Returns the file-browser node, creating and wiring everything on the first call.
   *
   * <p>On first call:
   *
   * <ol>
   *   <li>Loads {@code FileBrowserController.fxml}.
   *   <li>Creates a {@link FileBrowserViewModel} backed by the {@link DataStore}.
   *   <li>Initialises the controller with the ViewModel.
   *   <li>Binds the header Up button and path label to the ViewModel.
   * </ol>
   */
  private Node requireFileBrowserNode() {
    if (fileBrowserNode == null) {
      URL fxml = requireResource(FileBrowserController.class, "FileBrowserController.fxml");
      FXMLLoader loader = new FXMLLoader(fxml);
      try {
        Node node = loader.load();
        FileBrowserController controller = loader.getController();
        if (controller == null) {
          throw new IllegalStateException("FileBrowserController was not set by FXMLLoader");
        }
        FileBrowserViewModel fbViewModel = new FileBrowserViewModel(requireDataStore());

        restoreBrowserState(fbViewModel);

        controller.init(fbViewModel);

        fileBrowserNode = node;
        fileBrowserController = controller;
        fileBrowserViewModel = fbViewModel;

        wireHeaderNav(fbViewModel, controller);

        persistBrowserStateOn(fbViewModel);

        // Wire project-open event: double-clicking a ProjectEntry triggers loading.
        fbViewModel
            .openingProjectProperty()
            .addListener(
                (obs, old, entry) -> {
                  if (entry != null) {
                    loadAndShowProject(entry.path());
                  }
                });
      } catch (IOException e) {
        throw new IllegalStateException("Failed to load FileBrowserController.fxml", e);
      }
    }
    Node node = fileBrowserNode;
    if (node == null) {
      throw new IllegalStateException("fileBrowserNode was not initialized");
    }
    return node;
  }

  /**
   * Applies the saved browser view mode and directory path from {@link UserPreferences} to {@code
   * vm} before the controller is initialised. Falls back silently to defaults if the saved values
   * are absent, invalid, or point to a directory that no longer exists inside the workspace.
   */
  private void restoreBrowserState(FileBrowserViewModel vm) {
    UserPreferences savedPrefs = prefs;
    if (savedPrefs == null) {
      return;
    }

    String savedMode = savedPrefs.browserViewMode();
    if (savedMode != null) {
      try {
        vm.setViewMode(FileBrowserViewMode.valueOf(savedMode));
      } catch (IllegalArgumentException ignored) {
        // Unrecognised name — keep the default LIST mode.
      }
    }

    Path savedPath = savedPrefs.browserPath();
    if (savedPath != null) {
      try {
        Path rootPath = requireDataStore().getRootPath();
        if (savedPath.startsWith(rootPath) && Files.isDirectory(savedPath)) {
          vm.setCurrentPath(savedPath);
        }
      } catch (Exception ignored) {
        // IO error — keep the default root path.
      }
    }
  }

  /**
   * Adds listeners on {@code vm} that persist the browser view mode and current directory path to
   * {@link UserPreferences} whenever they change. Called once after the controller is wired so the
   * initial-restore writes do not trigger unnecessary saves.
   */
  private void persistBrowserStateOn(FileBrowserViewModel vm) {
    vm.viewModeProperty()
        .addListener(
            (obs, old, mode) -> {
              UserPreferences p = prefs;
              if (p != null) {
                p.setBrowserViewMode(mode.name());
                p.save();
              }
            });

    vm.currentPathProperty()
        .addListener(
            (obs, old, path) -> {
              UserPreferences p = prefs;
              if (p != null) {
                p.setBrowserPath(path);
                p.save();
              }
            });
  }

  /**
   * Binds the header navigation widgets to the {@link FileBrowserViewModel}.
   *
   * <ul>
   *   <li>Path label text tracks {@code currentPathProperty()} and is formatted relative to the
   *       workspace root.
   *   <li>Up button calls {@link FileBrowserController#navigateUp()}.
   *   <li>Up button is disabled when the browser is already at the workspace root.
   * </ul>
   */
  private void wireHeaderNav(FileBrowserViewModel vm, FileBrowserController controller) {
    DataStore ds = requireDataStore();

    // Path label: format as "~/.affr" or "~/.affr/sub/path"
    requireHeaderNavPathLabel()
        .textProperty()
        .bind(
            Bindings.createStringBinding(
                () -> PathFormatting.breadcrumb(vm.getCurrentPath(), ds.getRootPath()),
                vm.currentPathProperty()));

    // Up button action
    requireHeaderNavUpButton().setOnAction(e -> controller.navigateUp());

    // Up button disabled at root
    requireHeaderNavUpButton()
        .disableProperty()
        .bind(Bindings.createBooleanBinding(vm::isAtRoot, vm.currentPathProperty()));
  }

  private static URL requireResource(Class<?> owner, String resourceName) {
    URL url = owner.getResource(resourceName);
    if (url == null) {
      throw new IllegalStateException(resourceName + " not found on classpath");
    }
    return url;
  }

  // ── Project loading and navigation ───────────────────────────────────────

  /**
   * Starts a background task that loads the project at {@code projectPath} and on success calls
   * {@link #showProject(AFFrProject)} on the JavaFX Application Thread.
   */
  private void loadAndShowProject(Path projectPath) {
    Task<AFFrProject> task =
        new Task<>() {
          @Override
          protected AFFrProject call() throws Exception {
            return new ProjectLoader().load(projectPath);
          }
        };

    task.setOnSucceeded(
        e -> {
          @Nullable AFFrProject project = task.getValue();
          if (project != null) {
            showProject(project);
          }
        });

    task.setOnFailed(
        e -> {
          @Nullable Throwable ex = task.getException();
          System.err.println(
              "Failed to load project: " + (ex != null ? ex.getMessage() : "unknown error"));
          // Reset so the user can retry.
          FileBrowserViewModel fbVm = fileBrowserViewModel;
          if (fbVm != null) {
            fbVm.setOpeningProject(null);
          }
        });

    Thread.ofVirtual().name("affr-project-loader").start(task);
  }

  /**
   * Loads and displays the {@link ProjectController} for the given project. Hides the category list
   * and shows the back/home navigation buttons.
   */
  @SuppressWarnings("nullness") // BorderPane.setLeft(null) is the JavaFX API to clear the region
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

    // Swap layout: hide category list, show back/home nav, display project view.
    requireRootPane().setLeft(null);
    requireHeaderNav().setVisible(false);
    requireHeaderBackNav().setVisible(true);
    requireHeaderBackNav().setManaged(true);
    requireAppTitleLabel().setVisible(false);
    requireAppTitleLabel().setManaged(false);
    requireViewerPane().getChildren().setAll(node);
  }

  /**
   * Restores the browser layout: shows the category list, hides back/home nav, and re-renders the
   * viewer for the currently active category.
   */
  private void showBrowser() {
    requireRootPane().setLeft(requireCategoryList());
    requireHeaderBackNav().setVisible(false);
    requireHeaderBackNav().setManaged(false);
    requireAppTitleLabel().setVisible(true);
    requireAppTitleLabel().setManaged(true);
    renderViewer(requireViewModel().getSelectedCategory());

    // Reset so the same project can be opened again.
    FileBrowserViewModel fbVm = fileBrowserViewModel;
    if (fbVm != null) {
      fbVm.setOpeningProject(null);
    }
  }

  private void syncLanguageMenu(Locale locale) {
    boolean isJa = Locale.JAPANESE.getLanguage().equals(locale.getLanguage());
    requireLangJaItem().setSelected(isJa);
    requireLangEnItem().setSelected(!isJa);
  }

  // ── Null-guard helpers ────────────────────────────────────────────────────

  private BorderPane requireRootPane() {
    BorderPane pane = rootPane;
    if (pane == null) throw new IllegalStateException("rootPane not injected");
    return pane;
  }

  private ListView<TopCategory> requireCategoryList() {
    ListView<TopCategory> list = categoryList;
    if (list == null) throw new IllegalStateException("categoryList not injected");
    return list;
  }

  private StackPane requireViewerPane() {
    StackPane pane = viewerPane;
    if (pane == null) throw new IllegalStateException("viewerPane not injected");
    return pane;
  }

  private HBox requireHeaderNav() {
    HBox nav = headerNav;
    if (nav == null) throw new IllegalStateException("headerNav not injected");
    return nav;
  }

  private Button requireHeaderNavUpButton() {
    Button btn = headerNavUpButton;
    if (btn == null) throw new IllegalStateException("headerNavUpButton not injected");
    return btn;
  }

  private Label requireHeaderNavPathLabel() {
    Label lbl = headerNavPathLabel;
    if (lbl == null) throw new IllegalStateException("headerNavPathLabel not injected");
    return lbl;
  }

  private HBox requireHeaderBackNav() {
    HBox nav = headerBackNav;
    if (nav == null) throw new IllegalStateException("headerBackNav not injected");
    return nav;
  }

  private Button requireHeaderBackButton() {
    Button btn = headerBackButton;
    if (btn == null) throw new IllegalStateException("headerBackButton not injected");
    return btn;
  }

  private Button requireHeaderHomeButton() {
    Button btn = headerHomeButton;
    if (btn == null) throw new IllegalStateException("headerHomeButton not injected");
    return btn;
  }

  private Label requireAppTitleLabel() {
    Label lbl = appTitleLabel;
    if (lbl == null) throw new IllegalStateException("appTitleLabel not injected");
    return lbl;
  }

  private RadioMenuItem requireLangEnItem() {
    RadioMenuItem item = langEnItem;
    if (item == null) throw new IllegalStateException("langEnItem not injected");
    return item;
  }

  private RadioMenuItem requireLangJaItem() {
    RadioMenuItem item = langJaItem;
    if (item == null) throw new IllegalStateException("langJaItem not injected");
    return item;
  }

  private TopViewModel requireViewModel() {
    TopViewModel vm = viewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }

  private DataStore requireDataStore() {
    DataStore ds = dataStore;
    if (ds == null) throw new IllegalStateException("init() has not been called");
    return ds;
  }
}
