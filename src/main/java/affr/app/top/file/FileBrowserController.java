package affr.app.top.file;

import affr.data.BrowserEntry;
import affr.data.FolderEntry;
import affr.data.ProjectEntry;
import affr.fx.viewmodel.top.file.FileBrowserViewModel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * FXML controller for the File-browser content area (FILE category).
 *
 * <p>This controller owns the item list and loading state. Navigation controls (Up button,
 * breadcrumb path label) live in the app header and are managed by {@link
 * affr.app.top.TopController}, which calls {@link #navigateUp()} and binds to the ViewModel's
 * {@code currentPathProperty()} directly.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Bind the {@link ListView} to the ViewModel's item list.
 *   <li>Schedule background IO via {@link Task} when navigating.
 *   <li>Expose {@link #navigateUp()} so the app header's Up button can trigger it.
 * </ul>
 */
public final class FileBrowserController {

  // ── FXML-injected widgets ──────────────────────────────────────────────────

  @FXML private @Nullable ListView<BrowserEntry> itemList;
  @FXML private @Nullable ProgressIndicator loadingIndicator;
  @FXML private @Nullable Label emptyLabel;

  // ── ViewModel (set by init()) ──────────────────────────────────────────────

  private @Nullable FileBrowserViewModel viewModel;

  // ── FXML lifecycle ─────────────────────────────────────────────────────────

  @FXML
  private void initialize() {
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

    // Double-click navigates into the selected entry.
    requireItemList()
        .setOnMouseClicked(
            event -> {
              if (event.getClickCount() == 2) {
                BrowserEntry selected = requireItemList().getSelectionModel().getSelectedItem();
                if (selected != null) {
                  navigateTo(selected.path());
                }
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
                change ->
                    requireEmptyLabel()
                        .setVisible(viewModel.getItems().isEmpty() && !viewModel.isLoading()));

    requireItemList()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener((obs, old, sel) -> viewModel.setSelectedItem(sel));

    navigateTo(viewModel.getCurrentPath());
  }

  /**
   * Navigates up to the parent directory (clamped to the workspace root).
   *
   * <p>Called by the app header's Up button, which is wired by {@link affr.app.top.TopController}.
   */
  public void navigateUp() {
    FileBrowserViewModel vm = requireViewModel();
    if (!vm.isAtRoot()) {
      navigateTo(vm.parentPath());
    }
  }

  // ── Private helpers ────────────────────────────────────────────────────────

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

  private FileBrowserViewModel requireViewModel() {
    FileBrowserViewModel vm = viewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }
}
