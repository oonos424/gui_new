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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * FXML controller for the File-browser content area (FILE category).
 *
 * <p>Supports two display modes controlled by {@link FileBrowserViewMode}:
 *
 * <ul>
 *   <li>{@code LIST} — compact {@link ListView} (default).
 *   <li>{@code ICON} — grid of labelled icon tiles in a {@link FlowPane}.
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
  @FXML private @Nullable ProgressIndicator loadingIndicator;
  @FXML private @Nullable Label emptyLabel;
  @FXML private @Nullable Button newProjectButton;
  @FXML private @Nullable ToggleButton listModeButton;
  @FXML private @Nullable ToggleButton iconModeButton;

  // ── Controller state ───────────────────────────────────────────────────────

  private @Nullable FileBrowserViewModel viewModel;

  // Auto-select this entry name after the next directory reload (set before navigateTo).
  private @Nullable String pendingSelectName;

  // The icon tile currently highlighted in ICON mode; null when nothing is selected.
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

    // Double-click in list view: navigate into a folder, or signal project-open.
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

    // Wire view-mode toggle group (prevents all-deselected state).
    requireListModeButton().setToggleGroup(viewModeGroup);
    requireIconModeButton().setToggleGroup(viewModeGroup);
    viewModeGroup
        .selectedToggleProperty()
        .addListener(
            (obs, old, selected) -> {
              if (selected == null && old != null) {
                // Re-select the previous toggle; user clicked the already-active button.
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
              }
            });

    // ViewModel → toggle buttons + content swap
    viewModel.viewModeProperty().addListener((obs, old, mode) -> applyViewMode(mode));

    // Sync initial state (ViewModel may already have a non-default mode).
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

  /**
   * Applies the given view mode: swaps the visible content and rebuilds the icon pane if needed.
   */
  private void applyViewMode(FileBrowserViewMode mode) {
    switch (mode) {
      case LIST -> {
        requireListViewContainer().setVisible(true);
        requireIconScrollPane().setVisible(false);
      }
      case ICON -> {
        requireListViewContainer().setVisible(false);
        requireIconScrollPane().setVisible(true);
        updateIconView();
      }
    }
  }

  /** Moves the ToggleButton selection to match the given mode without firing the listener. */
  private void syncToggleToViewMode(FileBrowserViewMode mode) {
    switch (mode) {
      case LIST -> requireListModeButton().setSelected(true);
      case ICON -> requireIconModeButton().setSelected(true);
    }
  }

  // ── Icon view ─────────────────────────────────────────────────────────────

  /** Rebuilds the icon pane from the current ViewModel items. */
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
          navigateTo(currentPath);
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

    Thread thread = new Thread(task, "affr-project-creator");
    thread.setDaemon(true);
    thread.start();
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
          vm.setItems(task.getValue());
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

    Thread thread = new Thread(task, "affr-data-loader");
    thread.setDaemon(true);
    thread.start();
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

  private FileBrowserViewModel requireViewModel() {
    FileBrowserViewModel vm = viewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }
}
