package affr.app.top;

import affr.app.LanguageMenu;
import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.util.i18n.I18n;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
 * categories (FILE, RUNNING, TUTORIALS) and the workspace tab bar.
 *
 * <p>Owns only widgets and the bindings that connect them to the {@link TopViewModel} and to the
 * active sub-view's {@link FileBrowserViewModel}. Holds no application services and contains no IO;
 * project loading and screen routing live in {@link affr.app.NavigationService}.
 *
 * <p>The tab bar contains a pinned primary "Browser" tab and zero or more closeable secondary tabs
 * (e.g. Input Editor). Secondary tabs are opened via {@link #openTab(String, Node, Runnable)} and
 * closed via {@link #closeTab(int)}. The primary tab (index 0) is never closeable.
 *
 * <p>Lifecycle: FXML loading injects the widgets, then {@link #initialize()} runs (rendering rules
 * only), then {@link #init(TopViewModel, FileBrowserViewModel, Node)} wires bindings.
 *
 * <p>Project-mode transitions for the primary tab are exposed as {@link #enterProjectMode(Node)}
 * and {@link #exitProjectMode()} so {@code NavigationService} can request the layout swap without
 * owning the widget references itself.
 */
public final class TopController {

  // -------------------------------------------------------------------------
  // FXML-injected widgets
  // -------------------------------------------------------------------------

  @FXML private @Nullable BorderPane rootPane;
  @FXML private @Nullable ListView<TopCategory> categoryList;
  @FXML private @Nullable StackPane viewerPane;
  @FXML private @Nullable MenuItem settingMenuItem;
  @FXML private @Nullable HBox tabBar;
  @FXML private @Nullable RadioMenuItem langEnItem;
  @FXML private @Nullable RadioMenuItem langJaItem;

  // Header navigation controls (File-browser breadcrumb)
  @FXML private @Nullable HBox headerNav;
  @FXML private @Nullable Button headerNavUpButton;
  @FXML private @Nullable Label headerNavPathLabel;

  // Header back / home navigation (shown when a project is open in the primary tab)
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
  private @Nullable FileBrowserViewModel tutorialBrowserViewModel;
  private @Nullable Node tutorialBrowserNode;

  // Currently active project view in the primary tab (set in enterProjectMode).
  private @Nullable Node projectNode;

  // -------------------------------------------------------------------------
  // Tab state
  // -------------------------------------------------------------------------

  /** All open tabs in display order. Index 0 is always the pinned primary (Browser) tab. */
  private final List<TabEntry> tabs = new ArrayList<>();

  private int activeTabIndex = 0;

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
   *     navigation controls when FILE is active
   * @param fileBrowserNode the root node of the FILE-category sub-view; placed into the viewer pane
   *     when the FILE category is active
   * @param tutorialBrowserViewModel the ViewModel of the TUTORIALS-category sub-view, or {@code
   *     null} if no tutorial inventory is available
   * @param tutorialBrowserNode the root node of the TUTORIALS-category sub-view, or {@code null} if
   *     no tutorial inventory is available
   */
  public void init(
      TopViewModel viewModel,
      FileBrowserViewModel fileBrowserViewModel,
      Node fileBrowserNode,
      @Nullable FileBrowserViewModel tutorialBrowserViewModel,
      @Nullable Node tutorialBrowserNode) {
    this.viewModel = viewModel;
    this.fileBrowserViewModel = fileBrowserViewModel;
    this.fileBrowserNode = fileBrowserNode;
    this.tutorialBrowserViewModel = tutorialBrowserViewModel;
    this.tutorialBrowserNode = tutorialBrowserNode;

    // ── Initialize primary tab ────────────────────────────────────────
    tabs.add(new TabEntry(I18n.get("workspace.tab.browser"), fileBrowserNode, null));
    renderTabBar();

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
              if (activeTabIndex == 0) {
                renderViewer(selected);
              }
            });

    list.getSelectionModel().select(viewModel.getSelectedCategory());
    renderViewer(viewModel.getSelectedCategory());

    // ── Language menu ────────────────────────────────────────────────
    LanguageMenu.install(requireLangEnItem(), requireLangJaItem());

    // ── Refresh widgets when the locale changes ──────────────────────
    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              // Update primary tab title
              if (!tabs.isEmpty()) {
                tabs.set(
                    0,
                    new TabEntry(I18n.get("workspace.tab.browser"), tabs.get(0).content(), null));
              }
              renderTabBar();
              requireCategoryList().refresh();
              if (activeTabIndex == 0) {
                renderViewer(requireViewModel().getSelectedCategory());
              }
            });

    // ── Back / Home navigation (primary tab, project mode) ───────────
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
   * Swaps the primary tab into project mode: hides the category list, shows the back/home
   * navigation, and replaces the viewer pane contents with {@code projectNode}.
   *
   * <p>Only affects layout when the primary tab is active. Called by {@link
   * affr.app.NavigationService} once a project finishes loading.
   */
  @SuppressWarnings("nullness") // BorderPane.setLeft(null) is the JavaFX API to clear the region
  public void enterProjectMode(Node projectNode) {
    this.projectNode = projectNode;
    if (activeTabIndex == 0) {
      requireRootPane().setLeft(null);
      requireHeaderNav().setVisible(false);
      requireHeaderBackNav().setVisible(true);
      requireHeaderBackNav().setManaged(true);
      requireAppTitleLabel().setVisible(false);
      requireAppTitleLabel().setManaged(false);
      requireViewerPane().getChildren().setAll(projectNode);
    }
  }

  /**
   * Opens a new secondary tab in the workspace tab bar.
   *
   * <p>Called by {@link affr.app.NavigationService} when the user opens a calculation or any other
   * item that requires its own closeable tab (Input Editor, Mesh Viewer, etc.).
   *
   * @param title label shown on the tab button
   * @param content root node of the tab's content
   * @param onClose callback invoked when the tab is closed programmatically (e.g. from Back/Home
   *     actions inside the content); the × button on the tab calls {@link #closeTab(int)} directly
   */
  public void openTab(String title, Node content, Runnable onClose) {
    tabs.add(new TabEntry(title, content, onClose));
    renderTabBar();
    selectTab(tabs.size() - 1);
  }

  /**
   * Closes the tab at {@code index} and activates an adjacent tab.
   *
   * <p>No-op if {@code index} is 0 (the primary tab is pinned) or out of range.
   *
   * @param index the tab index to close (1-based for secondary tabs)
   */
  public void closeTab(int index) {
    if (index <= 0 || index >= tabs.size()) return;
    tabs.remove(index);
    renderTabBar();
    selectTab(Math.min(index, tabs.size() - 1));
  }

  /**
   * Returns the tab index whose content node is identical (by reference) to {@code content}, or
   * {@code -1} if not found.
   *
   * <p>Used by {@link affr.app.NavigationService} to resolve a content node back to a tab index for
   * programmatic close via {@link #closeTab(int)}.
   */
  public int indexOfNode(Node content) {
    for (int i = 0; i < tabs.size(); i++) {
      if (tabs.get(i).content() == content) return i;
    }
    return -1;
  }

  /**
   * Restores the primary tab to browser layout: shows the category list, hides back/home nav,
   * re-renders the viewer for the currently active category, closes any open secondary tabs, and
   * resets the file-browser VM so the same project can be re-opened.
   *
   * <p>Bound to the Back / Home buttons in the primary-tab header. Also called by {@link
   * affr.app.NavigationService} when project mode is exited programmatically.
   */
  @SuppressWarnings("nullness") // BorderPane.setLeft(null) is the JavaFX API to clear the region
  public void exitProjectMode() {
    this.projectNode = null;

    // Close all secondary tabs (going home dismisses all open item tabs).
    while (tabs.size() > 1) {
      tabs.remove(tabs.size() - 1);
    }
    activeTabIndex = 0;
    renderTabBar();

    requireRootPane().setLeft(requireCategoryList());
    requireHeaderBackNav().setVisible(false);
    requireHeaderBackNav().setManaged(false);
    requireAppTitleLabel().setVisible(true);
    requireAppTitleLabel().setManaged(true);
    requireHeaderNav().setVisible(false);
    renderViewer(requireViewModel().getSelectedCategory());

    // Reset so the same project can be opened again.
    FileBrowserViewModel fbVm = fileBrowserViewModel;
    if (fbVm != null) {
      fbVm.setOpeningProject(null);
      fbVm.clearOpenedProject();
    }
    FileBrowserViewModel tutVm = tutorialBrowserViewModel;
    if (tutVm != null) {
      tutVm.setOpeningProject(null);
      tutVm.clearOpenedProject();
    }
  }

  // -------------------------------------------------------------------------
  // Private helpers — tab management
  // -------------------------------------------------------------------------

  /**
   * Switches the active tab to {@code index}, updating the viewer content and sidebar visibility.
   *
   * <p>Primary tab (index 0): restores the browser or project view depending on whether the primary
   * tab is currently in project mode. Secondary tabs: hides the sidebar and shows the tab content.
   * The outer header navigation (breadcrumb / Back+Home) is also updated to match.
   */
  @SuppressWarnings("nullness") // BorderPane.setLeft(null) is the JavaFX API to clear the region
  private void selectTab(int index) {
    if (index < 0 || index >= tabs.size()) return;
    activeTabIndex = index;
    renderTabBar();

    if (index == 0) {
      // Restore primary tab state
      Node project = projectNode;
      if (project != null) {
        // Primary tab is in project mode
        requireRootPane().setLeft(null);
        requireHeaderNav().setVisible(false);
        requireHeaderBackNav().setVisible(true);
        requireHeaderBackNav().setManaged(true);
        requireAppTitleLabel().setVisible(false);
        requireAppTitleLabel().setManaged(false);
        requireViewerPane().getChildren().setAll(project);
      } else {
        // Primary tab is in browser mode
        requireRootPane().setLeft(requireCategoryList());
        requireHeaderBackNav().setVisible(false);
        requireHeaderBackNav().setManaged(false);
        requireAppTitleLabel().setVisible(true);
        requireAppTitleLabel().setManaged(true);
        renderViewer(requireViewModel().getSelectedCategory());
      }
    } else {
      // Secondary tab: hide sidebar and outer header nav
      requireRootPane().setLeft(null);
      requireHeaderNav().setVisible(false);
      requireHeaderBackNav().setVisible(false);
      requireHeaderBackNav().setManaged(false);
      requireAppTitleLabel().setVisible(true);
      requireAppTitleLabel().setManaged(true);
      requireViewerPane().getChildren().setAll(tabs.get(index).content());
    }
  }

  /** Rebuilds the tab bar children from the current {@link #tabs} list. */
  private void renderTabBar() {
    HBox bar = requireTabBar();
    bar.getChildren().clear();
    for (int i = 0; i < tabs.size(); i++) {
      bar.getChildren().add(buildTabButton(tabs.get(i), i));
    }
  }

  /**
   * Builds a single tab button widget for {@code entry} at position {@code index}.
   *
   * <p>Each tab is an {@code HBox} containing a label (and an × button for closeable tabs). Clicks
   * on the tab body activate the tab; clicks on × close it.
   */
  private Node buildTabButton(TabEntry entry, int index) {
    Label label = new Label(entry.title());
    label.getStyleClass().add("tab-item-label");

    HBox tab = new HBox(label);
    tab.getStyleClass().add("tab-item");
    if (index == activeTabIndex) {
      tab.getStyleClass().add("tab-item-selected");
    }

    Runnable onClose = entry.onClose();
    if (onClose != null) {
      Button closeBtn = new Button("×");
      closeBtn.getStyleClass().add("tab-close-btn");
      final int capturedIndex = index;
      closeBtn.setOnAction(
          e -> {
            e.consume();
            closeTab(capturedIndex);
          });
      tab.getChildren().add(closeBtn);
    }

    final int capturedIndex = index;
    tab.setOnMouseClicked(e -> selectTab(capturedIndex));
    return tab;
  }

  // -------------------------------------------------------------------------
  // Private helpers — viewer
  // -------------------------------------------------------------------------

  /**
   * Renders the viewer pane and updates the header navigation area for the given category.
   *
   * <p>FILE: shows the file-browser node and the header nav (Up + breadcrumb), binding nav to the
   * file-browser VM. TUTORIALS: shows the tutorial-browser node and header nav if the tutorial
   * inventory is available; otherwise shows a "not configured" placeholder. RUNNING: shows a
   * placeholder and hides the header nav.
   */
  private void renderViewer(TopCategory category) {
    StackPane pane = requireViewerPane();
    switch (category) {
      case FILE -> {
        requireHeaderNav().setVisible(true);
        wireHeaderNav(requireFileBrowserViewModel(), "~/.affr");
        pane.getChildren().setAll(requireFileBrowserNode());
      }
      case TUTORIALS -> {
        Node tutNode = tutorialBrowserNode;
        FileBrowserViewModel tutVm = tutorialBrowserViewModel;
        if (tutNode != null && tutVm != null) {
          requireHeaderNav().setVisible(true);
          wireHeaderNav(tutVm, "tutorials");
          pane.getChildren().setAll(tutNode);
        } else {
          requireHeaderNav().setVisible(false);
          Label placeholder = new Label(I18n.get("tutorials.notConfigured"));
          placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #888;");
          pane.getChildren().setAll(placeholder);
        }
      }
      case RUNNING -> {
        requireHeaderNav().setVisible(false);
        Label placeholder = new Label(I18n.get(category.messageKey()));
        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #888;");
        pane.getChildren().setAll(placeholder);
      }
    }
  }

  /**
   * Binds the header navigation widgets to the given {@link FileBrowserViewModel}. Safe to call
   * multiple times: each call rebinds to the new VM (previous bindings are replaced). Called from
   * {@link #renderViewer} when switching to a browser-surface category.
   *
   * <ul>
   *   <li>Path label tracks {@code currentPathProperty()} formatted relative to the workspace root,
   *       using {@code rootLabel} as the display name for the root.
   *   <li>Up button is disabled when the browser is at the workspace root.
   * </ul>
   */
  private void wireHeaderNav(FileBrowserViewModel vm, String rootLabel) {
    Path rootPath = vm.getRootPath();

    requireHeaderNavPathLabel()
        .textProperty()
        .bind(
            Bindings.createStringBinding(
                () -> PathFormatting.breadcrumb(vm.getCurrentPath(), rootPath, rootLabel),
                vm.currentPathProperty()));

    requireHeaderNavUpButton().setOnAction(e -> vm.navigateUp());
    requireHeaderNavUpButton()
        .disableProperty()
        .bind(Bindings.createBooleanBinding(vm::isAtRoot, vm.currentPathProperty()));
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

  private HBox requireTabBar() {
    HBox bar = tabBar;
    if (bar == null) throw new IllegalStateException("tabBar not injected");
    return bar;
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

  private FileBrowserViewModel requireFileBrowserViewModel() {
    FileBrowserViewModel vm = fileBrowserViewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }
}
