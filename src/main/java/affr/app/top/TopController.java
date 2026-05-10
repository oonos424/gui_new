package affr.app.top;

import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.util.i18n.I18n;
import java.nio.file.Path;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * View controller for the Project Browser shell — the persistent chrome around all top-level
 * categories (FILE, RUNNING, TUTORIALS).
 *
 * <p>Owns only widgets and the bindings that connect them to the {@link TopViewModel} and to the
 * active sub-view's {@link FileBrowserViewModel}. Holds no application services and contains no IO;
 * project loading and screen routing live in {@link affr.app.NavigationService}.
 *
 * <p>Lifecycle: FXML loading injects the widgets, then {@link #initialize()} runs (rendering rules
 * only), then {@link #init(TopViewModel, FileBrowserViewModel, Node)} wires bindings.
 *
 * <p>Project-mode transitions are exposed as {@link #enterProjectMode(Node)} and {@link
 * #exitProjectMode()} so {@code NavigationService} can request the layout swap without owning the
 * widget references itself.
 */
public final class TopController {

  // -------------------------------------------------------------------------
  // FXML-injected widgets
  // -------------------------------------------------------------------------

  @FXML private @Nullable BorderPane rootPane;
  @FXML private @Nullable ListView<TopCategory> categoryList;
  @FXML private @Nullable StackPane viewerPane;
  @FXML private @Nullable MenuItem settingMenuItem;
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
  // ViewModel + sub-view wiring (set by init())
  // -------------------------------------------------------------------------

  private @Nullable TopViewModel viewModel;
  private @Nullable FileBrowserViewModel fileBrowserViewModel;
  private @Nullable Node fileBrowserNode;

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
   * Wires all bindings between widgets, the {@link TopViewModel}, and the file-browser sub-view.
   *
   * @param viewModel the ViewModel for this shell
   * @param fileBrowserViewModel the ViewModel of the FILE-category sub-view; bound to the header
   *     navigation controls
   * @param fileBrowserNode the root node of the FILE-category sub-view; placed into the viewer pane
   *     when the FILE category is active
   */
  public void init(
      TopViewModel viewModel, FileBrowserViewModel fileBrowserViewModel, Node fileBrowserNode) {
    this.viewModel = viewModel;
    this.fileBrowserViewModel = fileBrowserViewModel;
    this.fileBrowserNode = fileBrowserNode;

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

    list.getSelectionModel().select(viewModel.getSelectedCategory());
    renderViewer(viewModel.getSelectedCategory());

    // ── Header navigation (binds directly to file-browser VM, no controller hop) ─
    wireHeaderNav(fileBrowserViewModel);

    // ── Language menu ────────────────────────────────────────────────
    syncLanguageMenu(I18n.getLocale());
    requireLangEnItem().setOnAction(e -> I18n.setLocale(Locale.ENGLISH));
    requireLangJaItem().setOnAction(e -> I18n.setLocale(Locale.JAPANESE));

    // ── Refresh widgets and language radio when locale changes ───────
    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              requireCategoryList().refresh();
              renderViewer(requireViewModel().getSelectedCategory());
              syncLanguageMenu(I18n.getLocale());
            });

    // ── Back / Home navigation ───────────────────────────────────────
    requireHeaderBackButton().setOnAction(e -> exitProjectMode());
    requireHeaderHomeButton().setOnAction(e -> exitProjectMode());
  }

  /**
   * Registers the action to run when the user clicks the "Setting" menu item. Must be called after
   * FXML injection (i.e. after the controller is loaded by {@link javafx.fxml.FXMLLoader}).
   */
  public void setOnSettingAction(Runnable action) {
    requireSettingMenuItem().setOnAction(e -> action.run());
  }

  /**
   * Swaps the layout into project mode: hides the category list, shows the back/home navigation,
   * and replaces the viewer pane contents with {@code projectNode}.
   *
   * <p>Called by {@link affr.app.NavigationService} once a project finishes loading.
   */
  @SuppressWarnings("nullness") // BorderPane.setLeft(null) is the JavaFX API to clear the region
  public void enterProjectMode(Node projectNode) {
    requireRootPane().setLeft(null);
    requireHeaderNav().setVisible(false);
    requireHeaderBackNav().setVisible(true);
    requireHeaderBackNav().setManaged(true);
    requireAppTitleLabel().setVisible(false);
    requireAppTitleLabel().setManaged(false);
    requireViewerPane().getChildren().setAll(projectNode);
  }

  /**
   * Restores the browser layout: shows the category list, hides back/home nav, re-renders the
   * viewer for the currently active category, and resets the file-browser VM so the same project
   * can be re-opened.
   *
   * <p>Bound to the Back / Home buttons in the header. Also called by {@link
   * affr.app.NavigationService} when project mode is exited programmatically.
   */
  public void exitProjectMode() {
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
      fbVm.clearOpenedProject();
    }
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Renders the viewer pane and updates the header navigation area for the given category.
   *
   * <p>FILE: shows the file-browser node and the header nav controls (Up + breadcrumb). RUNNING /
   * TUTORIALS: shows a placeholder label and hides the header nav.
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
   * Binds the header navigation widgets to the {@link FileBrowserViewModel}. The Up button calls
   * {@link FileBrowserViewModel#navigateUp()} directly — no controller-to-controller hop.
   *
   * <ul>
   *   <li>Path label tracks {@code currentPathProperty()} formatted relative to the workspace root.
   *   <li>Up button is disabled when the browser is at the workspace root.
   * </ul>
   */
  private void wireHeaderNav(FileBrowserViewModel vm) {
    Path rootPath = vm.getRootPath();

    requireHeaderNavPathLabel()
        .textProperty()
        .bind(
            Bindings.createStringBinding(
                () -> PathFormatting.breadcrumb(vm.getCurrentPath(), rootPath),
                vm.currentPathProperty()));

    requireHeaderNavUpButton().setOnAction(e -> vm.navigateUp());
    requireHeaderNavUpButton()
        .disableProperty()
        .bind(Bindings.createBooleanBinding(vm::isAtRoot, vm.currentPathProperty()));
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

  private MenuItem requireSettingMenuItem() {
    MenuItem item = settingMenuItem;
    if (item == null) throw new IllegalStateException("settingMenuItem not injected");
    return item;
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

  private Node requireFileBrowserNode() {
    Node node = fileBrowserNode;
    if (node == null) throw new IllegalStateException("init() has not been called");
    return node;
  }
}
