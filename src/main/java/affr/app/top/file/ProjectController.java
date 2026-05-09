package affr.app.top.file;

import affr.fx.viewmodel.top.file.ProjectSortOrder;
import affr.fx.viewmodel.top.file.ProjectViewModel;
import affr.project.CalculationItem;
import affr.project.CalculationStatus;
import affr.project.ProjectItem;
import affr.util.i18n.I18n;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
    requireItemList().setCellFactory(lv -> new ProjectItemCell());
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
            });
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

  private ProjectViewModel requireViewModel() {
    ProjectViewModel vm = viewModel;
    if (vm == null) throw new IllegalStateException("init() has not been called");
    return vm;
  }

  // ── Cell factory ──────────────────────────────────────────────────────────

  /**
   * List cell that renders a {@link ProjectItem} via an exhaustive pattern-matching switch.
   *
   * <p>When a new {@link ProjectItem} subtype is permitted, a new {@code case} must be added here —
   * the compiler enforces this through the sealed hierarchy.
   */
  private static final class ProjectItemCell extends ListCell<ProjectItem> {

    @Override
    protected void updateItem(@Nullable ProjectItem item, boolean empty) {
      super.updateItem(item, empty);
      if (item == null || empty) {
        setText(null);
        setGraphic(null);
        setStyle("");
        return;
      }
      switch (item) {
        case CalculationItem c -> renderCalculation(c);
      }
    }

    private void renderCalculation(CalculationItem c) {
      String statusLabel = I18n.get(c.status().messageKey());
      String datePart = c.date().isEmpty() ? "" : "  ·  " + c.date();
      setText(c.name() + "\n" + statusLabel + datePart);
      setStyle(statusColorStyle(c.status()));
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
