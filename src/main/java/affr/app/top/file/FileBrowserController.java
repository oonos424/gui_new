package affr.app.top.file;

import affr.data.BrowserEntry;
import affr.data.DataStore;
import affr.data.FolderEntry;
import affr.data.ProjectEntry;
import affr.fx.viewmodel.top.file.FileBrowserViewMode;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import affr.util.i18n.I18n;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * FXML controller for the File-browser content area (FILE category).
 *
 * <p>Supports three display modes controlled by {@link FileBrowserViewMode}:
 *
 * <ul>
 *   <li>{@code LIST} — compact {@link ListView} (default).
 *   <li>{@code ICON} — grid of labelled icon tiles in a {@link FlowPane}.
 *   <li>{@code TREE} — hierarchical {@link TreeView} with lazy child loading.
 * </ul>
 *
 * <p>Navigation controls (Up button, breadcrumb path label) live in the app header and are managed
 * by {@link affr.app.top.TopController}.
 */
public final class FileBrowserController {

  // ── FXML-injected widgets ──────────────────────────────────────────────────

  @FXML private @Nullable ListView<BrowserEntry> itemList;
  @FXML private @Nullable StackPane listViewContainer;
  @FXML private @Nullable ScrollPane iconScrollPane;
  @FXML private @Nullable FlowPane iconPane;
  @FXML private @Nullable TreeView<BrowserEntry> treeView;
  @FXML private @Nullable ProgressIndicator loadingIndicator;
  @FXML private @Nullable Label emptyLabel;
  @FXML private @Nullable Button newProjectButton;
  @FXML private @Nullable ToggleButton listModeButton;
  @FXML private @Nullable ToggleButton iconModeButton;
  @FXML private @Nullable ToggleButton treeModeButton;

  // ── Controller state ───────────────────────────────────────────────────────

  private @Nullable FileBrowserViewModel viewModel;

  // Auto-select this entry name after the next directory reload.
  private @Nullable String pendingSelectName;

  // The icon tile currently highlighted in ICON mode.
  private @Nullable Node currentIconTile;

  // Created programmatically so we can enforce "always one selected" invariant.
  private final ToggleGroup viewModeGroup = new ToggleGroup();

  // ── FXML lifecycle ─────────────────────────────────────────────────────────

  @FXML
  private void initialize() {
    // List-view cell factory
    requireItemList()
        .setCellFactory(
            lv ->
                new ListCell<>() {
                  @Override
                  protected void updateItem(@Nullable BrowserEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                      setText(null);
                      setGraphic(null);
                      return;
                    }
                    switch (item) {
                      case ProjectEntry p -> {
                        setText("📁  " + p.name());
                        setStyle("-fx-font-weight: bold;");
                      }
                      case FolderEntry f -> {
                        setText("📂  " + f.name());
                        setStyle("");
                      }
                    }
                  }
                });

    // Double-click in list view
    requireItemList()
        .setOnMouseClicked(
            event -> {
              if (event.getClickCount() == 2) {
                BrowserEntry selected = requireItemList().getSelectionModel().getSelectedItem();
                if (selected != null) {
                  switch (selected) {
                    case FolderEntry f -> navigateTo(f.path());
                    case ProjectEntry p -> requireViewModel().setOpeningProject(p);
                  }
                }
              }
            });

    // Tree-view cell factory
    requireTreeView()
        .setCellFactory(
            tv ->
                new TreeCell<>() {
                  @Override
                  protected void updateItem(@Nullable BrowserEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                      setText(null);
                      setStyle("");
                      return;
                    }
                    switch (item) {
                      case ProjectEntry p -> {
                        setText("📁  " + p.name());
                        setStyle("-fx-font-weight: bold;");
                      }
                      case FolderEntry f -> {
                        setText("📂  " + f.name());
                        setStyle("");
                      }
                    }
                  }
                });

    // Wire view-mode toggle group (prevents all-deselected state).
    requireListModeButton().setToggleGroup(viewModeGroup);
    requireIconModeButton().setToggleGroup(viewModeGroup);
    requireTreeModeButton().setToggleGroup(viewModeGroup);
    viewModeGroup
        .selectedToggleProperty()
        .addListener(
            (obs, old, selected) -> {
              if (selected == null && old != null) {
                old.setSelected(true);
              }
            });
  }

  // ── Public API ─────────────────────────────────────────────────────────────

  /**
   * Wires widget bindings to the ViewModel and triggers the initial directory load.
   *
   * @param viewModel the ViewModel for this view
   */
  public void init(FileBrowserViewModel viewModel) {
    this.viewModel = viewModel;

    requireItemList().setItems(viewModel.getItems());
    requireLoadingIndicator().visibleProperty().bind(viewModel.loadingProperty());

    viewModel
        .getItems()
        .addListener(
            (ListChangeListener<BrowserEntry>)
                change -> {
                  requireEmptyLabel()
                      .setVisible(viewModel.getItems().isEmpty() && !viewModel.isLoading());
                  if (viewModel.getViewMode() == FileBrowserViewMode.ICON) {
                    updateIconView();
                  }
                });

    requireItemList()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, old, sel) -> viewModel.setSelectedItem(sel));

    // Tree view: selection → ViewModel; double-click on project → open
    requireTreeView()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, treeItem) -> {
              if (treeItem != null) {
                BrowserEntry entry = treeItem.getValue();
                viewModel.setSelectedItem(entry);
                // Keep currentPath in sync so New Project uses the selected folder's path.
                if (entry instanceof FolderEntry f) {
                  viewModel.setCurrentPath(f.path());
                }
              } else {
                viewModel.setSelectedItem(null);
              }
            });

    requireTreeView()
        .setOnMouseClicked(
            event -> {
              if (event.getClickCount() == 2) {
                @Nullable TreeItem<BrowserEntry> item =
                    requireTreeView().getSelectionModel().getSelectedItem();
                if (item != null && item.getValue() instanceof ProjectEntry p) {
                  viewModel.setOpeningProject(p);
                }
                // FolderEntry double-click: tree's default expand/collapse handles it.
              }
            });

    // ── View-mode wiring ───────────────────────────────────────────────────

    // ToggleButton → ViewModel
    viewModeGroup
        .selectedToggleProperty()
        .addListener(
            (obs, old, toggle) -> {
              if (toggle == null) return;
              if (toggle == requireListModeButton()) {
                viewModel.setViewMode(FileBrowserViewMode.LIST);
              } else if (toggle == requireIconModeButton()) {
                viewModel.setViewMode(FileBrowserViewMode.ICON);
              } else if (toggle == requireTreeModeButton()) {
                viewModel.setViewMode(FileBrowserViewMode.TREE);
              }
            });

    // ViewModel → toggle buttons + content swap
    viewModel.viewModeProperty().addListener((obs, old, mode) -> applyViewMode(mode));

    // Sync initial state
    syncToggleToViewMode(viewModel.getViewMode());
    applyViewMode(viewModel.getViewMode());

    // ── New-project button label ───────────────────────────────────────────
    requireNewProjectButton().setText(I18n.get("browser.newProjectButton"));
    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) ->
                requireNewProjectButton().setText(I18n.get("browser.newProjectButton")));

    navigateTo(viewModel.getCurrentPath());
  }

  /**
   * Navigates up to the parent directory (clamped to the workspace root).
   *
   * <p>Called by the app header's Up button, wired by {@link affr.app.top.TopController}.
   */
  public void navigateUp() {
    FileBrowserViewModel vm = requireViewModel();
    if (!vm.isAtRoot()) {
      navigateTo(vm.parentPath());
    }
  }

  // ── View-mode switching ────────────────────────────────────────────────────

  private void applyViewMode(FileBrowserViewMode mode) {
    switch (mode) {
      case LIST -> {
        requireListViewContainer().setVisible(true);
        requireIconScrollPane().setVisible(false);
        requireTreeView().setVisible(false);
      }
      case ICON -> {
        requireListViewContainer().setVisible(false);
        requireIconScrollPane().setVisible(true);
        requireTreeView().setVisible(false);
        updateIconView();
      }
      case TREE -> {
        requireListViewContainer().setVisible(false);
        requireIconScrollPane().setVisible(false);
        requireTreeView().setVisible(true);
        initTreeView();
      }
    }
  }

  private void syncToggleToViewMode(FileBrowserViewMode mode) {
    switch (mode) {
      case LIST -> requireListModeButton().setSelected(true);
      case ICON -> requireIconModeButton().setSelected(true);
      case TREE -> requireTreeModeButton().setSelected(true);
    }
  }

  // ── Tree view ──────────────────────────────────────────────────────────────

  /**
   * Builds the tree root the first time the user switches to TREE mode. Subsequent switches reuse
   * the same root so expanded/collapsed state is preserved.
   */
  private void initTreeView() {
    TreeView<BrowserEntry> tree = requireTreeView();
    if (tree.getRoot() != null) {
      return; // Already built — preserve expanded state across mode switches.
    }

    DataStore ds = requireViewModel().getDataStore();
    Path rootPath = ds.getRootPath();
    @Nullable Path rootFileName = rootPath.getFileName();
    String rootName = rootFileName != null ? rootFileName.toString() : rootPath.toString();

    LazyTreeItem root = new LazyTreeItem(new FolderEntry(rootPath, rootName), ds);
    tree.setRoot(root);
    root.setExpanded(true); // Immediately loads the workspace root's children.
  }

  /**
   * A {@link TreeItem} that loads its children from disk on first expansion. The loading happens on
   * a background thread; children are populated on the JavaFX Application Thread once ready.
   *
   * <p>Projects are always leaves. Folders show the expand arrow until clicked; if a folder turns
   * out to be empty, the expansion simply shows nothing.
   */
  private static final class LazyTreeItem extends TreeItem<BrowserEntry> {

    private boolean loaded = false;

    LazyTreeItem(BrowserEntry entry, DataStore dataStore) {
      super(entry);
      if (!(entry instanceof ProjectEntry)) {
        // Trigger a background load whenever this node is expanded for the first time.
        expandedProperty()
            .addListener(
                (obs, wasExpanded, isExpanded) -> {
                  if (Boolean.TRUE.equals(isExpanded) && !loaded) {
                    loaded = true;
                    loadChildrenAsync(dataStore);
                  }
                });
      }
    }

    @Override
    public boolean isLeaf() {
      // Projects never have sub-items in the browser tree.
      return getValue() instanceof ProjectEntry;
    }

    /**
     * Clears current children and reloads from disk. Used after creating a new project inside this
     * folder so the tree reflects the change without collapsing unrelated nodes.
     */
    void reload(DataStore dataStore) {
      loaded = true; // Prevent the expansion listener from triggering a second load.
      getChildren().clear();
      loadChildrenAsync(dataStore);
    }

    private void loadChildrenAsync(DataStore dataStore) {
      Path path = getValue().path();

      Task<List<BrowserEntry>> task =
          new Task<>() {
            @Override
            protected List<BrowserEntry> call() throws Exception {
              return dataStore.loadChildren(path);
            }
          };

      task.setOnSucceeded(
          e -> {
            @Nullable List<BrowserEntry> children = task.getValue();
            if (children != null) {
              getChildren()
                  .setAll(
                      children.stream().map(child -> new LazyTreeItem(child, dataStore)).toList());
            }
          });

      task.setOnFailed(
          e -> {
            // Leave children empty; the expand arrow stays but shows nothing.
          });

      Thread.ofVirtual().name("affr-tree-loader").start(task);
    }
  }

  // ── Icon view ─────────────────────────────────────────────────────────────

  private void updateIconView() {
    FlowPane pane = requireIconPane();
    pane.getChildren().clear();
    currentIconTile = null;

    for (BrowserEntry entry : requireViewModel().getItems()) {
      pane.getChildren().add(createIconTile(entry));
    }
  }

  private Node createIconTile(BrowserEntry entry) {
    String emoji =
        switch (entry) {
          case ProjectEntry p -> "📁";
          case FolderEntry f -> "📂";
        };

    Label iconLabel = new Label(emoji);
    iconLabel.setStyle("-fx-font-size: 36px;");

    Label nameLabel = new Label(entry.name());
    nameLabel.setMaxWidth(84.0);
    nameLabel.setWrapText(true);
    nameLabel.setTextAlignment(TextAlignment.CENTER);
    nameLabel.setStyle("-fx-font-size: 11px;");

    VBox tile = new VBox(4.0, iconLabel, nameLabel);
    tile.setAlignment(Pos.CENTER);
    tile.setPrefWidth(96.0);
    tile.setPrefHeight(86.0);
    tile.setPadding(new Insets(6.0));
    tile.getStyleClass().add("icon-tile");
    tile.setUserData(entry);

    tile.setOnMouseClicked(
        event -> {
          selectIconTile(tile, entry);
          if (event.getClickCount() == 2) {
            switch (entry) {
              case FolderEntry f -> navigateTo(f.path());
              case ProjectEntry p -> requireViewModel().setOpeningProject(p);
            }
          }
        });

    return tile;
  }

  private void selectIconTile(Node tile, BrowserEntry entry) {
    if (currentIconTile != null) {
      currentIconTile.getStyleClass().remove("icon-tile-selected");
    }
    tile.getStyleClass().add("icon-tile-selected");
    currentIconTile = tile;
    requireViewModel().setSelectedItem(entry);
  }

  // ── New-project dialog ────────────────────────────────────────────────────

  @FXML
  private void onNewProject() {
    @Nullable NewProjectParams params = showNewProjectDialog();
    if (params == null) {
      return;
    }

    FileBrowserViewModel vm = requireViewModel();
    DataStore ds = vm.getDataStore();
    Path currentPath = vm.getCurrentPath();

    Task<ProjectEntry> task =
        new Task<>() {
          @Override
          protected ProjectEntry call() throws Exception {
            return ds.createProject(currentPath, params.name(), params.memo());
          }
        };

    task.setOnSucceeded(
        e -> {
          @Nullable ProjectEntry created = task.getValue();
          pendingSelectName = created != null ? created.name() : null;
          if (vm.getViewMode() == FileBrowserViewMode.TREE) {
            // In tree mode, refresh the selected folder's subtree instead of the flat list.
            refreshTreeNodeForPath(currentPath);
          } else {
            navigateTo(currentPath);
          }
        });

    task.setOnFailed(
        e -> {
          @Nullable Throwable ex = task.getException();
          String message = ex != null ? ex.getMessage() : I18n.get("dialog.error.title");
          Alert alert = new Alert(Alert.AlertType.ERROR);
          alert.setTitle(I18n.get("dialog.error.title"));
          alert.setContentText(message != null ? message : "");
          alert.showAndWait();
        });

    Thread.ofVirtual().name("affr-project-creator").start(task);
  }

  /**
   * Reloads the children of the {@link LazyTreeItem} whose path matches {@code dirPath}. This
   * refreshes the tree in-place after a project is created without collapsing the rest of the tree.
   */
  private void refreshTreeNodeForPath(Path dirPath) {
    TreeView<BrowserEntry> tree = requireTreeView();
    @Nullable TreeItem<BrowserEntry> root = tree.getRoot();
    if (root == null) return;

    @Nullable LazyTreeItem target = findTreeItem(root, dirPath);
    if (target != null) {
      target.reload(requireViewModel().getDataStore());
    }
    // Auto-select the new project after the reload completes (handled inside reload()).
    pendingSelectName = null; // reload() will do its own selection
  }

  /**
   * Finds the {@link LazyTreeItem} in the subtree rooted at {@code node} whose entry path equals
   * {@code targetPath}, or {@code null} if not found.
   */
  private @Nullable LazyTreeItem findTreeItem(TreeItem<BrowserEntry> node, Path targetPath) {
    if (node.getValue().path().equals(targetPath) && node instanceof LazyTreeItem lti) {
      return lti;
    }
    for (TreeItem<BrowserEntry> child : node.getChildren()) {
      @Nullable LazyTreeItem found = findTreeItem(child, targetPath);
      if (found != null) return found;
    }
    return null;
  }

  private @Nullable NewProjectParams showNewProjectDialog() {
    TextField nameField = new TextField();
    nameField.setPromptText(I18n.get("dialog.newProject.namePlaceholder"));

    TextArea memoArea = new TextArea();
    memoArea.setPrefRowCount(3);
    memoArea.setWrapText(true);

    VBox content =
        new VBox(
            6.0,
            new Label(I18n.get("dialog.newProject.nameLabel")),
            nameField,
            new Label(I18n.get("dialog.newProject.memoLabel")),
            memoArea);
    content.setPrefWidth(340.0);
    content.setPadding(new Insets(12.0, 0.0, 0.0, 0.0));

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle(I18n.get("dialog.newProject.title"));
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dialog.setResultConverter(bt -> bt == ButtonType.OK ? ButtonType.OK : ButtonType.CANCEL);

    @Nullable Node okNode = dialog.getDialogPane().lookupButton(ButtonType.OK);
    if (okNode instanceof Button okBtn) {
      okBtn.setDisable(true);
      nameField.textProperty().addListener((obs, old, val) -> okBtn.setDisable(val.isBlank()));
    }

    nameField.requestFocus();

    Optional<@Nullable ButtonType> result = dialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      String name = nameField.getText().strip();
      if (!name.isBlank()) {
        return new NewProjectParams(name, memoArea.getText().strip());
      }
    }
    return null;
  }

  private record NewProjectParams(String name, String memo) {}

  // ── Navigation ────────────────────────────────────────────────────────────

  private void navigateTo(Path path) {
    FileBrowserViewModel vm = requireViewModel();
    vm.setLoading(true);
    vm.setCurrentPath(path);
    vm.getItems().clear();
    requireEmptyLabel().setVisible(false);

    Task<List<BrowserEntry>> task =
        new Task<>() {
          @Override
          protected List<BrowserEntry> call() throws Exception {
            return vm.getDataStore().loadChildren(path);
          }
        };

    task.setOnSucceeded(
        e -> {
          List<BrowserEntry> loaded = task.getValue();
          vm.setItems(loaded != null ? loaded : List.of());
          vm.setLoading(false);
          requireEmptyLabel().setVisible(vm.getItems().isEmpty());

          @Nullable String nameToSelect = pendingSelectName;
          pendingSelectName = null;
          if (nameToSelect != null) {
            final String target = nameToSelect;
            vm.getItems().stream()
                .filter(entry -> entry.name().equals(target))
                .findFirst()
                .ifPresent(entry -> requireItemList().getSelectionModel().select(entry));
          }
        });

    task.setOnFailed(
        e -> {
          vm.setItems(Collections.emptyList());
          vm.setLoading(false);
          requireEmptyLabel().setVisible(true);
        });

    Thread.ofVirtual().name("affr-data-loader").start(task);
  }

  // ── Null-guard helpers ────────────────────────────────────────────────────

  private ListView<BrowserEntry> requireItemList() {
    ListView<BrowserEntry> list = itemList;
    if (list == null) throw new IllegalStateException("itemList not injected by FXMLLoader");
    return list;
  }

  private StackPane requireListViewContainer() {
    StackPane sp = listViewContainer;
    if (sp == null) throw new IllegalStateException("listViewContainer not injected by FXMLLoader");
    return sp;
  }

  private ScrollPane requireIconScrollPane() {
    ScrollPane sp = iconScrollPane;
    if (sp == null) throw new IllegalStateException("iconScrollPane not injected by FXMLLoader");
    return sp;
  }

  private FlowPane requireIconPane() {
    FlowPane fp = iconPane;
    if (fp == null) throw new IllegalStateException("iconPane not injected by FXMLLoader");
    return fp;
  }

  private TreeView<BrowserEntry> requireTreeView() {
    TreeView<BrowserEntry> tv = treeView;
    if (tv == null) throw new IllegalStateException("treeView not injected by FXMLLoader");
    return tv;
  }

  private ProgressIndicator requireLoadingIndicator() {
    ProgressIndicator pi = loadingIndicator;
    if (pi == null) throw new IllegalStateException("loadingIndicator not injected by FXMLLoader");
    return pi;
  }

  private Label requireEmptyLabel() {
    Label label = emptyLabel;
    if (label == null) throw new IllegalStateException("emptyLabel not injected by FXMLLoader");
    return label;
  }

  private Button requireNewProjectButton() {
    Button btn = newProjectButton;
    if (btn == null) throw new IllegalStateException("newProjectButton not injected by FXMLLoader");
    return btn;
  }

  private ToggleButton requireListModeButton() {
    ToggleButton btn = listModeButton;
    if (btn == null) throw new IllegalStateException("listModeButton not injected by FXMLLoader");
    return btn;
  }

  private ToggleButton requireIconModeButton() {
    ToggleButton btn = iconModeButton;
    if (btn == null) throw new IllegalStateException("iconModeButton not injected by FXMLLoader");
    return btn;
  }

  private ToggleButton requireTreeModeButton() {
    ToggleButton btn = treeModeButton;
    if (btn == null) throw new IllegalStateException("treeModeButton not injected by FXMLLoader");
    return btn;
  }

  private FileBrowserViewModel requireViewModel() {
    FileBrowserViewModel vm = viewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }
}
