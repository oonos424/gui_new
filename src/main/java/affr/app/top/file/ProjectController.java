package affr.app.top.file;

import affr.fx.viewmodel.top.file.ProjectSortOrder;
import affr.fx.viewmodel.top.file.ProjectViewModel;
import affr.project.AFFrCalculation;
import affr.project.AFFrCalculationModel;
import affr.project.AFFrProject;
import affr.project.CalculationStatus;
import affr.project.ProjectItem;
import affr.project.ProjectWriter;
import affr.util.i18n.I18n;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.util.StringConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * FXML controller for the Project Item List screen (MANAGER + VIEWER layout).
 *
 * <p>Owns the item {@link ListView}, the sort {@link ChoiceBox}, the project-name label, and the
 * add-item button. The VIEWER area is a placeholder in this phase; it will be populated by the
 * Execution domain in a later phase.
 *
 * <p>Lifecycle: FXML loading injects the widgets; {@link #initialize()} sets rendering rules only;
 * then the caller invokes {@link #init(ProjectViewModel)} to wire data bindings.
 */
public final class ProjectController {

  // ── FXML-injected widgets ──────────────────────────────────────────────────

  @FXML private @Nullable Label projectNameLabel;
  @FXML private @Nullable ChoiceBox<ProjectSortOrder> sortChoiceBox;
  @FXML private @Nullable ListView<ProjectItem> itemList;
  @FXML private @Nullable Button addItemButton;

  // ── ViewModel (set by init()) ──────────────────────────────────────────────

  private @Nullable ProjectViewModel viewModel;

  // ── FXML lifecycle ─────────────────────────────────────────────────────────

  @FXML
  private void initialize() {
    ListView<ProjectItem> list = requireItemList();
    list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    list.setCellFactory(lv -> new ProjectItemCell());
  }

  // ── Public API ─────────────────────────────────────────────────────────────

  /**
   * Wires all bindings between widgets and the ViewModel.
   *
   * @param viewModel the ViewModel for this project
   */
  public void init(ProjectViewModel viewModel) {
    this.viewModel = viewModel;

    requireProjectNameLabel().setText(viewModel.getProjectName());
    requireItemList().setItems(viewModel.getSortedItems());

    requireAddItemButton().setText(I18n.get("project.addCalButton"));

    // ListView → ViewModel: user clicks an item
    requireItemList()
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, sel) -> {
              if (!Objects.equals(sel, viewModel.getFocusedItem())) {
                viewModel.setFocusedItem(sel);
              }
            });

    // ViewModel → ListView: programmatic focus change (e.g. restored from persistence
    // in the Input Editor phase)
    viewModel
        .focusedItemProperty()
        .addListener(
            (obs, old, item) -> {
              if (!Objects.equals(item, requireItemList().getSelectionModel().getSelectedItem())) {
                if (item != null) {
                  requireItemList().getSelectionModel().select(item);
                } else {
                  requireItemList().getSelectionModel().clearSelection();
                }
              }
            });

    setupSortChoiceBox(viewModel);

    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              requireItemList().refresh();
              refreshSortChoiceBoxLabels();
              requireAddItemButton().setText(I18n.get("project.addCalButton"));
            });
  }

  // ── Add calculation ────────────────────────────────────────────────────────

  @FXML
  private void onAddCalculation() {
    Optional<AFFrCalculationModel> modelChoice = showNewCalculationDialog();
    if (modelChoice.isEmpty()) return;

    ProjectViewModel vm = requireViewModel();
    AFFrProject project = vm.getProject();
    Path projectPath = vm.getProjectPath();
    List<ProjectItem> snapshot = List.copyOf(vm.getProjectItems());
    AFFrCalculationModel model = modelChoice.get();

    Task<AFFrCalculation> task =
        new Task<>() {
          @Override
          protected AFFrCalculation call() throws Exception {
            return ProjectWriter.createCalculation(projectPath, snapshot, project, model);
          }
        };

    task.setOnSucceeded(
        e -> {
          @Nullable AFFrCalculation cal = task.getValue();
          if (cal != null) {
            vm.addItem(cal);
            requireItemList().getSelectionModel().select(cal);
          }
        });

    task.setOnFailed(e -> showError(task.getException()));

    Thread thread = new Thread(task, "affr-cal-creator");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Opens the New Calculation model-selection dialog and returns the user's choice.
   *
   * @return the selected {@link AFFrCalculationModel}, or empty if the user cancelled
   */
  private Optional<AFFrCalculationModel> showNewCalculationDialog() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("NewCalculationDialog.fxml"));
      DialogPane dialogPane = loader.load();
      NewCalculationDialogController ctrl = loader.getController();
      ctrl.setProjectPath(requireViewModel().getProjectPath());

      Dialog<AFFrCalculationModel> dialog = new Dialog<>();
      dialog.setTitle(I18n.get("newCal.dialog.title"));
      dialog.setDialogPane(dialogPane);
      dialog.initOwner(requireItemList().getScene().getWindow());
      dialog.setResultConverter(bt -> bt == ButtonType.OK ? ctrl.buildResult() : null);

      return dialog.showAndWait();
    } catch (IOException e) {
      showError(e);
      return Optional.empty();
    }
  }

  // ── Context menu handlers ──────────────────────────────────────────────────

  private void onRenameCalculation(AFFrCalculation cal) {
    ProjectViewModel vm = requireViewModel();

    TextInputDialog dialog = new TextInputDialog(cal.name());
    dialog.setTitle(I18n.get("project.rename.title"));
    dialog.setHeaderText(I18n.get("project.rename.header"));
    dialog.setContentText(null);

    Optional<String> result = dialog.showAndWait();
    if (result.isEmpty()) return;

    String newName = result.get().trim();
    if (newName.isBlank()) {
      showInlineError(I18n.get("project.rename.errorBlank"));
      return;
    }
    if (newName.equals(cal.name())) return;

    List<ProjectItem> snapshot = List.copyOf(vm.getProjectItems());
    Path projectPath = vm.getProjectPath();

    Task<AFFrCalculation> task =
        new Task<>() {
          @Override
          protected AFFrCalculation call() throws Exception {
            return ProjectWriter.renameCalculation(projectPath, cal, newName, snapshot);
          }
        };

    task.setOnSucceeded(
        e -> {
          @Nullable AFFrCalculation renamed = task.getValue();
          if (renamed != null) {
            vm.replaceItem(cal, renamed);
            requireItemList().getSelectionModel().select(renamed);
          }
        });

    task.setOnFailed(e -> showError(task.getException()));

    Thread thread = new Thread(task, "affr-cal-rename");
    thread.setDaemon(true);
    thread.start();
  }

  private void onCopyCalculation(AFFrCalculation cal) {
    ProjectViewModel vm = requireViewModel();
    AFFrProject project = vm.getProject();
    Path projectPath = vm.getProjectPath();
    List<ProjectItem> snapshot = List.copyOf(vm.getProjectItems());

    Task<AFFrCalculation> task =
        new Task<>() {
          @Override
          protected AFFrCalculation call() throws Exception {
            return ProjectWriter.copyCalculation(projectPath, cal, snapshot, project);
          }
        };

    task.setOnSucceeded(
        e -> {
          @Nullable AFFrCalculation copy = task.getValue();
          if (copy != null) {
            vm.addItem(copy);
            requireItemList().getSelectionModel().select(copy);
          }
        });

    task.setOnFailed(e -> showError(task.getException()));

    Thread thread = new Thread(task, "affr-cal-copy");
    thread.setDaemon(true);
    thread.start();
  }

  private void onDeleteCalculations(List<AFFrCalculation> targets) {
    ProjectViewModel vm = requireViewModel();
    if (targets.isEmpty()) return;

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle(I18n.get("project.delete.title"));
    if (targets.size() == 1) {
      confirm.setHeaderText(
          MessageFormat.format(I18n.get("project.delete.header"), targets.get(0).name()));
    } else {
      confirm.setHeaderText(
          MessageFormat.format(I18n.get("project.delete.headerMultiple"), targets.size()));
    }
    confirm.setContentText(I18n.get("project.delete.content"));

    Optional<ButtonType> result = confirm.showAndWait();
    if (result.isEmpty() || result.get() != ButtonType.OK) return;

    List<AFFrCalculation> deleted = new ArrayList<>();

    Task<Void> task =
        new Task<>() {
          @Override
          protected Void call() throws Exception {
            for (AFFrCalculation cal : targets) {
              ProjectWriter.deleteCalculation(cal);
              deleted.add(cal);
            }
            return null;
          }
        };

    task.setOnSucceeded(
        e -> {
          for (AFFrCalculation cal : deleted) vm.removeItem(cal);
        });

    task.setOnFailed(
        e -> {
          for (AFFrCalculation cal : deleted) vm.removeItem(cal);
          showError(task.getException());
        });

    Thread thread = new Thread(task, "affr-cal-delete");
    thread.setDaemon(true);
    thread.start();
  }

  private void onOpenInFinder(AFFrCalculation cal) {
    File dir = cal.path().toFile();
    Task<Void> task =
        new Task<>() {
          @Override
          protected Void call() throws Exception {
            Desktop.getDesktop().open(dir);
            return null;
          }
        };

    task.setOnFailed(e -> showError(task.getException()));

    Thread thread = new Thread(task, "affr-open-finder");
    thread.setDaemon(true);
    thread.start();
  }

  // ── Error helpers ──────────────────────────────────────────────────────────

  private void showError(@Nullable Throwable ex) {
    String message = ex != null ? ex.getMessage() : I18n.get("dialog.error.title");
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(I18n.get("dialog.error.title"));
    alert.setContentText(message != null ? message : "");
    alert.showAndWait();
  }

  private void showInlineError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(I18n.get("dialog.error.title"));
    alert.setContentText(message);
    alert.showAndWait();
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private void setupSortChoiceBox(ProjectViewModel vm) {
    ChoiceBox<ProjectSortOrder> box = requireSortChoiceBox();
    box.setConverter(makeSortConverter());
    box.getItems().setAll(ProjectSortOrder.values());
    box.getSelectionModel().select(vm.getSortOrder());

    // ChoiceBox → ViewModel
    box.valueProperty()
        .addListener(
            (obs, old, val) -> {
              if (val != null && !val.equals(vm.getSortOrder())) {
                vm.setSortOrder(val);
              }
            });

    // ViewModel → ChoiceBox (keeps the control in sync when sort order is changed
    // programmatically, e.g. from a keyboard shortcut or a reset action)
    vm.sortOrderProperty()
        .addListener(
            (obs, old, order) -> {
              if (!order.equals(box.getValue())) {
                box.getSelectionModel().select(order);
              }
            });
  }

  /** Forces the ChoiceBox to re-render its selected label with the new locale. */
  private void refreshSortChoiceBoxLabels() {
    ChoiceBox<ProjectSortOrder> box = requireSortChoiceBox();
    @Nullable ProjectSortOrder current = box.getValue();
    // Assigning a new converter instance triggers ChoiceBox to re-render the displayed label.
    box.setConverter(makeSortConverter());
    if (current != null) {
      box.setValue(current);
    }
  }

  private static StringConverter<ProjectSortOrder> makeSortConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(@Nullable ProjectSortOrder item) {
        return item != null ? I18n.get(item.messageKey()) : "";
      }

      @Override
      public ProjectSortOrder fromString(String s) {
        throw new UnsupportedOperationException();
      }
    };
  }

  // ── Null-guard helpers ────────────────────────────────────────────────────

  private Label requireProjectNameLabel() {
    Label l = projectNameLabel;
    if (l == null) throw new IllegalStateException("projectNameLabel not injected");
    return l;
  }

  private ChoiceBox<ProjectSortOrder> requireSortChoiceBox() {
    ChoiceBox<ProjectSortOrder> box = sortChoiceBox;
    if (box == null) throw new IllegalStateException("sortChoiceBox not injected");
    return box;
  }

  private ListView<ProjectItem> requireItemList() {
    ListView<ProjectItem> list = itemList;
    if (list == null) throw new IllegalStateException("itemList not injected");
    return list;
  }

  private Button requireAddItemButton() {
    Button btn = addItemButton;
    if (btn == null) throw new IllegalStateException("addItemButton not injected");
    return btn;
  }

  private ProjectViewModel requireViewModel() {
    ProjectViewModel vm = viewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }

  // ── Cell factory ──────────────────────────────────────────────────────────

  /**
   * List cell that renders a {@link ProjectItem} via an exhaustive pattern-matching switch and
   * attaches a right-click context menu with Rename, Copy, Delete, and Open in Finder actions.
   *
   * <p>When a new {@link ProjectItem} subtype is permitted, a new {@code case} must be added here —
   * the compiler enforces this through the sealed hierarchy.
   *
   * <p>Hover and selection highlighting follow the same Java-listener approach used by the category
   * selector in {@code TopController}: hover/selected listeners combine the status text-fill with
   * the hover background so that inline and CSS styles never conflict.
   *
   * <p>This is a non-static inner class so it can call context-menu handler methods on the
   * enclosing {@link ProjectController}.
   */
  private final class ProjectItemCell extends ListCell<ProjectItem> {

    private final MenuItem renameItem = new MenuItem();
    private final MenuItem copyItem = new MenuItem();
    private final MenuItem deleteItem = new MenuItem();
    private final MenuItem openInFinderItem = new MenuItem();
    private final ContextMenu contextMenu =
        new ContextMenu(renameItem, copyItem, deleteItem, openInFinderItem);

    private String statusStyle = "";

    ProjectItemCell() {
      hoverProperty().addListener((obs, old, hovered) -> applyCurrentStyle());
      selectedProperty().addListener((obs, old, selected) -> applyCurrentStyle());

      // Re-evaluate enabled state and wire actions to the live selection right before the menu
      // appears, so multi-select is always reflected correctly.
      contextMenu.setOnShowing(e -> refreshMenuState());
    }

    @Override
    protected void updateItem(@Nullable ProjectItem item, boolean empty) {
      super.updateItem(item, empty);
      if (item == null || empty) {
        setText(null);
        setGraphic(null);
        statusStyle = "";
        setStyle("");
        setContextMenu(null);
        return;
      }
      switch (item) {
        case AFFrCalculation c -> renderCalculation(c);
      }
    }

    private void renderCalculation(AFFrCalculation c) {
      String statusLabel = I18n.get(c.getStatus().messageKey());
      String datePart = c.date().isEmpty() ? "" : "  ·  " + c.date();
      setText(c.name() + "\n" + statusLabel + datePart);
      statusStyle = statusColorStyle(c.getStatus());
      applyCurrentStyle();

      renameItem.setText(I18n.get("project.contextMenu.rename"));
      copyItem.setText(I18n.get("project.contextMenu.copy"));
      deleteItem.setText(I18n.get("project.contextMenu.delete"));
      openInFinderItem.setText(I18n.get("project.contextMenu.openInFinder"));

      setContextMenu(contextMenu);
    }

    /**
     * Called just before the context menu is shown. Wires actions and disables single-item
     * operations when more than one item is selected.
     */
    private void refreshMenuState() {
      @Nullable ProjectItem item = getItem();
      if (item == null) {
        contextMenu.hide();
        return;
      }

      List<AFFrCalculation> selectedCals = selectedCalculations();
      boolean multi = selectedCals.size() > 1;

      renameItem.setDisable(multi);
      copyItem.setDisable(multi);
      openInFinderItem.setDisable(multi);

      if (item instanceof AFFrCalculation c) {
        renameItem.setOnAction(e -> onRenameCalculation(c));
        copyItem.setOnAction(e -> onCopyCalculation(c));
        openInFinderItem.setOnAction(e -> onOpenInFinder(c));
      }
      deleteItem.setOnAction(e -> onDeleteCalculations(selectedCals));
    }

    /** Returns all currently selected items that are {@link AFFrCalculation} instances. */
    private List<AFFrCalculation> selectedCalculations() {
      return getListView().getSelectionModel().getSelectedItems().stream()
          .filter(AFFrCalculation.class::isInstance)
          .map(AFFrCalculation.class::cast)
          .toList();
    }

    /**
     * Applies the correct inline style for the current hover/selection state.
     *
     * <ul>
     *   <li>Selected — clear inline style so CSS {@code :selected} rules handle both background and
     *       text-fill.
     *   <li>Hovered (not selected) — add hover background alongside the status text-fill.
     *   <li>Normal — status text-fill only; CSS default background applies.
     * </ul>
     */
    private void applyCurrentStyle() {
      if (isSelected()) {
        setStyle("");
      } else if (isHover()) {
        setStyle("-fx-background-color: #eaf4ff; " + statusStyle);
      } else {
        setStyle(statusStyle);
      }
    }

    private static String statusColorStyle(CalculationStatus status) {
      return switch (status) {
        case SETTING -> "-fx-text-fill: #888888;";
        case SETUP -> "-fx-text-fill: #2266aa;";
        case QUEUING -> "-fx-text-fill: #aa6600;";
        case CALCULATING -> "-fx-text-fill: #0066aa;";
        case CALCULATED -> "-fx-text-fill: #006633;";
        case CAL_ABORTED -> "-fx-text-fill: #cc2200;";
        case PRE_ABORTED -> "-fx-text-fill: #cc4400;";
      };
    }
  }
}
