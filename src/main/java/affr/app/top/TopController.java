package affr.app.top;

import affr.app.top.file.FileBrowserController;
import affr.data.DataStore;
import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioMenuItem;
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

  @FXML private @Nullable ListView<TopCategory> categoryList;
  @FXML private @Nullable StackPane viewerPane;
  @FXML private @Nullable RadioMenuItem langEnItem;
  @FXML private @Nullable RadioMenuItem langJaItem;

  // Header navigation controls (FILE category breadcrumb)
  @FXML private @Nullable HBox headerNav;
  @FXML private @Nullable Button headerNavUpButton;
  @FXML private @Nullable Label headerNavPathLabel;

  // -------------------------------------------------------------------------
  // ViewModel + services (set by init())
  // -------------------------------------------------------------------------

  private @Nullable TopViewModel viewModel;
  private @Nullable DataStore dataStore;

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
        controller.init(fbViewModel);

        fileBrowserNode = node;
        fileBrowserController = controller;
        fileBrowserViewModel = fbViewModel;

        wireHeaderNav(fbViewModel, controller);
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
                () -> formatBreadcrumb(vm.getCurrentPath(), ds.getRootPath()),
                vm.currentPathProperty()));

    // Up button action
    requireHeaderNavUpButton().setOnAction(e -> controller.navigateUp());

    // Up button disabled at root
    requireHeaderNavUpButton()
        .disableProperty()
        .bind(Bindings.createBooleanBinding(vm::isAtRoot, vm.currentPathProperty()));
  }

  /** Formats a path as {@code ~/.affr} or {@code ~/.affr/relative/sub/path}. */
  private static String formatBreadcrumb(Path current, Path root) {
    if (current.equals(root)) {
      return "~/.affr";
    }
    return "~/.affr/" + root.relativize(current);
  }

  private static URL requireResource(Class<?> owner, String resourceName) {
    URL url = owner.getResource(resourceName);
    if (url == null) {
      throw new IllegalStateException(resourceName + " not found on classpath");
    }
    return url;
  }

  private void syncLanguageMenu(Locale locale) {
    boolean isJa = Locale.JAPANESE.getLanguage().equals(locale.getLanguage());
    requireLangJaItem().setSelected(isJa);
    requireLangEnItem().setSelected(!isJa);
  }

  // ── Null-guard helpers ────────────────────────────────────────────────────

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
