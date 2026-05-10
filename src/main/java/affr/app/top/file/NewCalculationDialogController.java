package affr.app.top.file;

import affr.app.PathFx;
import affr.project.AFFrCalculationModel;
import affr.project.ComprsModel;
import affr.project.CtlReader;
import affr.project.ExtraModel;
import affr.project.ModelConstraints;
import affr.project.ProjectUiState;
import affr.project.SteadyModel;
import affr.project.TurbModel;
import affr.util.i18n.I18n;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Controller for the New Calculation model-selection dialog.
 *
 * <p>The dialog lets the user choose:
 *
 * <ul>
 *   <li>Flow type (compressibility) via {@link ComprsModel}
 *   <li>Steady / unsteady via {@link SteadyModel}
 *   <li>Turbulence approach via {@link TurbModel}
 *   <li>Optional model add-ons via {@link ExtraModel}
 * </ul>
 *
 * <p>Checkbox availability is driven by {@link ModelConstraints#allowedExtras}, which encodes the
 * domain rules about which combinations are valid.
 */
public final class NewCalculationDialogController {

  // ── FXML-injected widgets ──────────────────────────────────────────────────

  @FXML private @Nullable Label nameLabel;
  @FXML private @Nullable TextField nameField;
  @FXML private @Nullable Label flowLabel;
  @FXML private @Nullable Label steadyLabel;
  @FXML private @Nullable Label turbLabel;
  @FXML private @Nullable Label extrasLabel;
  @FXML private @Nullable ComboBox<ComprsModel> flowCombo;
  @FXML private @Nullable ComboBox<SteadyModel> steadyCombo;
  @FXML private @Nullable ComboBox<TurbModel> turbCombo;
  @FXML private @Nullable VBox vboxExtraLeft;
  @FXML private @Nullable VBox vboxExtraRight;
  @FXML private @Nullable Button readFromFileButton;

  /** Ordered list of checkboxes — index {@code i} corresponds to {@code ExtraModel.values()[i]}. */
  private final List<CheckBox> checkList = new ArrayList<>();

  /** Set by the caller before the dialog is shown. Used to seed the FileChooser directory. */
  private @Nullable Path projectPath;

  // ── FXML lifecycle ─────────────────────────────────────────────────────────

  @FXML
  private void initialize() {
    requireNameLabel().setText(I18n.get("newCal.calculationName"));
    requireFlowLabel().setText(I18n.get("newCal.flowType"));
    requireSteadyLabel().setText(I18n.get("newCal.steady"));
    requireTurbLabel().setText(I18n.get("newCal.turbulence"));
    requireExtrasLabel().setText(I18n.get("newCal.optionalModels"));
    requireReadFromFileButton().setText(I18n.get("newCal.readFromFile"));

    setupComboBox(requireFlowCombo(), ComprsModel.values(), "newCal.comprs.");
    setupComboBox(requireSteadyCombo(), SteadyModel.values(), "newCal.steady.");
    setupComboBox(requireTurbCombo(), TurbModel.values(), "newCal.turb.");

    requireFlowCombo().setValue(ComprsModel.INCOMPRESSIBLE);
    requireSteadyCombo().setValue(SteadyModel.STEADY);
    requireTurbCombo().setValue(TurbModel.RANS);

    setupExtraCheckBoxes();

    // Every combo change triggers a full constraint recompute
    requireFlowCombo().valueProperty().addListener((obs, old, val) -> updateCheckboxStates());
    requireSteadyCombo().valueProperty().addListener((obs, old, val) -> updateCheckboxStates());
    requireTurbCombo().valueProperty().addListener((obs, old, val) -> updateCheckboxStates());

    updateCheckboxStates();
  }

  // ── Public API ─────────────────────────────────────────────────────────────

  /**
   * Supplies the project directory to this controller. Must be called before the dialog is shown.
   *
   * <p>The path is used as the fallback starting directory for the "Read from file" FileChooser,
   * and also to locate the per-project {@link ProjectUiState} that remembers the last directory the
   * user navigated to for this project.
   */
  public void setProjectPath(Path path) {
    this.projectPath = path;
  }

  /**
   * Pre-fills the calculation-name text field with {@code name}. Must be called before the dialog
   * is shown so the user sees a sensible default they can either accept or override.
   */
  public void setDefaultName(String name) {
    requireNameField().setText(name);
  }

  /** Returns the trimmed calculation name currently entered by the user. */
  public String getName() {
    String text = requireNameField().getText();
    return text == null ? "" : text.trim();
  }

  /**
   * Observable text property of the calculation-name field. Exposed so callers can bind UI state
   * (e.g. enable/disable the Create button when the name is blank).
   */
  public ReadOnlyStringProperty nameProperty() {
    return requireNameField().textProperty();
  }

  /**
   * Builds an {@link AFFrCalculationModel} from the current control state.
   *
   * <p>Only enabled and selected checkboxes are included; disabled checkboxes are excluded even if
   * somehow left selected (defensive).
   */
  public AFFrCalculationModel buildResult() {
    Set<ExtraModel> extras = EnumSet.noneOf(ExtraModel.class);
    ExtraModel[] allExtras = ExtraModel.values();
    for (int i = 0; i < checkList.size(); i++) {
      CheckBox cb = checkList.get(i);
      if (cb.isSelected() && !cb.isDisabled()) {
        extras.add(allExtras[i]);
      }
    }
    return new AFFrCalculationModel(
        requireFlowCombo().getValue(),
        requireSteadyCombo().getValue(),
        requireTurbCombo().getValue(),
        extras.isEmpty() ? Set.of() : Set.copyOf(extras));
  }

  // ── Read from file ─────────────────────────────────────────────────────────

  @FXML
  private void onReadFromFile() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(I18n.get("newCal.readFromFile"));
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(I18n.get("newCal.readFromFile.filterDesc"), "*.ctl"));

    // Determine starting directory: project-scoped last-used dir, then project root, then none.
    @Nullable Path proj = projectPath;
    if (proj != null) {
      ProjectUiState uiState = ProjectUiState.load(proj);
      @Nullable File startDir = PathFx.toExistingDir(uiState.getCtlLastDir());
      chooser.setInitialDirectory(startDir != null ? startDir : PathFx.toFile(proj));
    }

    @Nullable File file =
        chooser.showOpenDialog(requireReadFromFileButton().getScene().getWindow());
    if (file == null) return;

    Path chosenPath = PathFx.fromChooser(file);

    // Persist the chosen directory back to the per-project UI state (fast properties-file write).
    if (proj != null) {
      Path chosenDir = chosenPath.getParent();
      if (chosenDir != null) {
        ProjectUiState uiState = ProjectUiState.load(proj);
        uiState.setCtlLastDir(chosenDir);
        uiState.save();
      }
    }

    // Parse the .ctl file off the JavaFX Application Thread.
    Button btn = requireReadFromFileButton();
    btn.setDisable(true);
    Path ctlPath = chosenPath;

    javafx.concurrent.Task<AFFrCalculationModel> task =
        new javafx.concurrent.Task<>() {
          @Override
          protected AFFrCalculationModel call() throws Exception {
            return CtlReader.read(ctlPath);
          }
        };

    task.setOnSucceeded(
        e -> {
          @Nullable AFFrCalculationModel model = task.getValue();
          if (model != null) populateFromModel(model);
          btn.setDisable(false);
        });

    task.setOnFailed(
        e -> {
          btn.setDisable(false);
          Alert alert = new Alert(Alert.AlertType.ERROR);
          alert.setContentText(I18n.get("newCal.readFromFile.error"));
          alert.initOwner(btn.getScene().getWindow());
          alert.showAndWait();
        });

    Thread.ofVirtual().name("affr-ctl-reader").start(task);
  }

  // ── Constraint engine ──────────────────────────────────────────────────────

  /**
   * Recomputes which extra-model checkboxes are enabled and applies the result.
   *
   * <p>Delegates the domain constraint logic to {@link ModelConstraints#allowedExtras}. Enable
   * checkboxes in the resulting set; disable and deselect all others. This method is idempotent.
   */
  private void updateCheckboxStates() {
    ExtraModel[] allExtras = ExtraModel.values();

    Set<ExtraModel> selected = EnumSet.noneOf(ExtraModel.class);
    for (int i = 0; i < checkList.size(); i++) {
      if (checkList.get(i).isSelected()) selected.add(allExtras[i]);
    }

    Set<ExtraModel> allowed =
        ModelConstraints.allowedExtras(
            requireFlowCombo().getValue(),
            requireSteadyCombo().getValue(),
            requireTurbCombo().getValue(),
            selected);

    for (int i = 0; i < checkList.size(); i++) {
      boolean enable = allowed.contains(allExtras[i]);
      CheckBox cb = checkList.get(i);
      if (!enable) cb.setSelected(false);
      cb.setDisable(!enable);
    }
  }

  // ── Populate from model (used by "Read from file") ─────────────────────────

  private void populateFromModel(AFFrCalculationModel model) {
    requireFlowCombo().setValue(model.comprsModel());
    requireSteadyCombo().setValue(model.steadyModel());
    requireTurbCombo().setValue(model.turbModel());
    // Combo listeners fire updateCheckboxStates(); set checkbox state after
    Set<ExtraModel> extras = model.extraModelSet();
    ExtraModel[] allExtras = ExtraModel.values();
    for (int i = 0; i < checkList.size(); i++) {
      CheckBox cb = checkList.get(i);
      if (!cb.isDisabled()) {
        cb.setSelected(extras.contains(allExtras[i]));
      }
    }
    // Re-run constraints to reflect the newly-selected extras
    updateCheckboxStates();
  }

  // ── Setup helpers ──────────────────────────────────────────────────────────

  private void setupExtraCheckBoxes() {
    ExtraModel[] allExtras = ExtraModel.values();
    int splitAt = allExtras.length / 2; // even 6 / 6 split
    for (int i = 0; i < allExtras.length; i++) {
      CheckBox cb = new CheckBox(I18n.get("newCal.extra." + allExtras[i].name()));
      cb.setDisable(true);
      checkList.add(cb);
      // Each checkbox change re-runs the full constraint engine
      cb.setOnAction(e -> updateCheckboxStates());
      if (i < splitAt) {
        requireVboxExtraLeft().getChildren().add(cb);
      } else {
        requireVboxExtraRight().getChildren().add(cb);
      }
    }
  }

  private static <T> void setupComboBox(ComboBox<T> box, T[] values, String keyPrefix) {
    box.getItems().setAll(values);
    box.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(@Nullable T item) {
            return item != null ? I18n.get(keyPrefix + item) : "";
          }

          @Override
          public T fromString(String s) {
            throw new UnsupportedOperationException();
          }
        });
  }

  // ── Null-guard accessors ───────────────────────────────────────────────────

  private Label requireNameLabel() {
    Label l = nameLabel;
    if (l == null) throw new IllegalStateException("nameLabel not injected");
    return l;
  }

  private TextField requireNameField() {
    TextField f = nameField;
    if (f == null) throw new IllegalStateException("nameField not injected");
    return f;
  }

  private Label requireFlowLabel() {
    Label l = flowLabel;
    if (l == null) throw new IllegalStateException("flowLabel not injected");
    return l;
  }

  private Label requireSteadyLabel() {
    Label l = steadyLabel;
    if (l == null) throw new IllegalStateException("steadyLabel not injected");
    return l;
  }

  private Label requireTurbLabel() {
    Label l = turbLabel;
    if (l == null) throw new IllegalStateException("turbLabel not injected");
    return l;
  }

  private Label requireExtrasLabel() {
    Label l = extrasLabel;
    if (l == null) throw new IllegalStateException("extrasLabel not injected");
    return l;
  }

  private ComboBox<ComprsModel> requireFlowCombo() {
    ComboBox<ComprsModel> c = flowCombo;
    if (c == null) throw new IllegalStateException("flowCombo not injected");
    return c;
  }

  private ComboBox<SteadyModel> requireSteadyCombo() {
    ComboBox<SteadyModel> c = steadyCombo;
    if (c == null) throw new IllegalStateException("steadyCombo not injected");
    return c;
  }

  private ComboBox<TurbModel> requireTurbCombo() {
    ComboBox<TurbModel> c = turbCombo;
    if (c == null) throw new IllegalStateException("turbCombo not injected");
    return c;
  }

  private VBox requireVboxExtraLeft() {
    VBox v = vboxExtraLeft;
    if (v == null) throw new IllegalStateException("vboxExtraLeft not injected");
    return v;
  }

  private VBox requireVboxExtraRight() {
    VBox v = vboxExtraRight;
    if (v == null) throw new IllegalStateException("vboxExtraRight not injected");
    return v;
  }

  private Button requireReadFromFileButton() {
    Button b = readFromFileButton;
    if (b == null) throw new IllegalStateException("readFromFileButton not injected");
    return b;
  }
}
