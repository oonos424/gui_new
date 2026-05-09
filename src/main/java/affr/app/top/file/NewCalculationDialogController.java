package affr.app.top.file;

import static affr.project.ComprsModel.COMPRESSIBLE;
import static affr.project.ComprsModel.INCOMPRESSIBLE;
import static affr.project.ExtraModel.CAVITATION;
import static affr.project.ExtraModel.COMBUSTION;
import static affr.project.ExtraModel.COMBUST_CHEM_REACT;
import static affr.project.ExtraModel.GHOST_FLUID;
import static affr.project.ExtraModel.MOVING_MESH;
import static affr.project.ExtraModel.OVERSET_GRID;
import static affr.project.ExtraModel.PARTICLE_TRACK;
import static affr.project.ExtraModel.POROUS_MODEL;
import static affr.project.ExtraModel.RADIATION;
import static affr.project.ExtraModel.ROTATING_FRAME;
import static affr.project.ExtraModel.SURFACE_REACTION;
import static affr.project.ExtraModel.VOF;
import static affr.project.SteadyModel.STEADY;
import static affr.project.SteadyModel.UNSTEADY;
import static affr.project.TurbModel.DNS;
import static affr.project.TurbModel.LES;
import static affr.project.TurbModel.NO;
import static affr.project.TurbModel.RANS;

import affr.project.AFFrCalculationModel;
import affr.project.ComprsModel;
import affr.project.CtlReader;
import affr.project.ExtraModel;
import affr.project.ProjectUiState;
import affr.project.SteadyModel;
import affr.project.TurbModel;
import affr.util.i18n.I18n;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
 * <h2>Constraint model</h2>
 *
 * <p>Two maps drive checkbox availability:
 *
 * <ol>
 *   <li>{@link #BASIC_ALLOWED} — for each (comprs, steady, turb) combination, the set of extra
 *       models that are fundamentally compatible.
 *   <li>{@link #EXTRA_COMPATIBLE} — for each extra model, the set of extras that remain compatible
 *       when that extra is selected. Selecting an extra restricts the available set to the
 *       intersection of all selected extras' compatibility sets.
 * </ol>
 *
 * <p>The two maps are intersected on every combo-box or checkbox change. This replaces the legacy
 * raw-integer-matrix approach with a readable, type-safe design that is easy to extend.
 */
public final class NewCalculationDialogController {

  // ── Compatibility data ────────────────────────────────────────────────────

  /**
   * Groups a (comprs, steady, turb) triple as a map key.
   *
   * <p>Using a record gives correct {@code equals}/{@code hashCode} for free.
   */
  private record ModeKey(ComprsModel comprs, SteadyModel steady, TurbModel turb) {}

  /**
   * For each (comprs, steady, turb) combination, the extras that may be enabled at all.
   *
   * <p>Any extra absent from the set for the current combination is always disabled, regardless of
   * what else is selected. Missing keys default to an empty set.
   */
  private static final Map<ModeKey, Set<ExtraModel>> BASIC_ALLOWED = buildBasicAllowed();

  /**
   * For each extra model, the set of extras that remain selectable when <em>that</em> extra is
   * selected. Selecting extra {@code A} restricts the enabled set to {@code
   * EXTRA_COMPATIBLE.get(A)}. When multiple extras are selected, the enabled set is the
   * intersection of all their compatibility sets, then additionally constrained by {@link
   * #BASIC_ALLOWED}.
   */
  private static final Map<ExtraModel, Set<ExtraModel>> EXTRA_COMPATIBLE = buildExtraCompatible();

  // ── FXML-injected widgets ──────────────────────────────────────────────────

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
    requireFlowLabel().setText(I18n.get("newCal.flowType"));
    requireSteadyLabel().setText(I18n.get("newCal.steady"));
    requireTurbLabel().setText(I18n.get("newCal.turbulence"));
    requireExtrasLabel().setText(I18n.get("newCal.optionalModels"));
    requireReadFromFileButton().setText(I18n.get("newCal.readFromFile"));

    setupComboBox(requireFlowCombo(), ComprsModel.values(), "newCal.comprs.");
    setupComboBox(requireSteadyCombo(), SteadyModel.values(), "newCal.steady.");
    setupComboBox(requireTurbCombo(), TurbModel.values(), "newCal.turb.");

    requireFlowCombo().setValue(INCOMPRESSIBLE);
    requireSteadyCombo().setValue(STEADY);
    requireTurbCombo().setValue(RANS);

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
      @Nullable Path lastDir = uiState.getCtlLastDir();
      File startDir =
          (lastDir != null && Files.isDirectory(lastDir)) ? lastDir.toFile() : proj.toFile();
      chooser.setInitialDirectory(startDir);
    }

    @Nullable File file =
        chooser.showOpenDialog(requireReadFromFileButton().getScene().getWindow());
    if (file == null) return;

    // Persist the chosen directory back to the per-project UI state.
    if (proj != null) {
      Path chosenDir = file.toPath().getParent();
      if (chosenDir != null) {
        ProjectUiState uiState = ProjectUiState.load(proj);
        uiState.setCtlLastDir(chosenDir);
        uiState.save();
      }
    }

    try {
      populateFromModel(CtlReader.read(file.toPath()));
    } catch (IOException e) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setHeaderText(null);
      alert.setContentText(I18n.get("newCal.readFromFile.error"));
      alert.initOwner(requireReadFromFileButton().getScene().getWindow());
      alert.showAndWait();
    }
  }

  // ── Constraint engine ──────────────────────────────────────────────────────

  /**
   * Recomputes which extra-model checkboxes are enabled and applies the result.
   *
   * <p>Algorithm:
   *
   * <ol>
   *   <li>Look up the base-allowed set for the current (comprs, steady, turb) combination.
   *   <li>Intersect with the compatibility sets of all currently-selected extras.
   *   <li>Enable checkboxes in the resulting set; disable and deselect all others.
   * </ol>
   *
   * <p>This method is idempotent and replaces the separate {@code triggerExtraModes} / {@code
   * triggerCompete} pattern from the legacy codebase.
   */
  private void updateCheckboxStates() {
    ModeKey key =
        new ModeKey(
            requireFlowCombo().getValue(),
            requireSteadyCombo().getValue(),
            requireTurbCombo().getValue());

    // Base set from the current required-field combination
    Set<ExtraModel> allowed = new HashSet<>(BASIC_ALLOWED.getOrDefault(key, Set.of()));

    // Narrow down by each currently-selected extra's compatibility set
    ExtraModel[] allExtras = ExtraModel.values();
    for (int i = 0; i < checkList.size(); i++) {
      if (checkList.get(i).isSelected()) {
        allowed.retainAll(EXTRA_COMPATIBLE.getOrDefault(allExtras[i], Set.of()));
      }
    }

    // Apply to checkboxes
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

  // ── Compatibility table builders ───────────────────────────────────────────

  /**
   * Defines which extras may be enabled for each (comprs, steady, turb) combination.
   *
   * <p>Values are derived from the legacy {@code BASIC_EXTRA} matrix, translated into type-safe
   * {@link EnumSet}s. Missing keys default to {@link Set#of()}.
   */
  private static Map<ModeKey, Set<ExtraModel>> buildBasicAllowed() {
    Map<ModeKey, Set<ExtraModel>> m = new HashMap<>();

    // ── Incompressible ──────────────────────────────────────────────────────
    // Steady: only combustion-family and rotating frame make sense
    Set<ExtraModel> incompSteady = extras(COMBUST_CHEM_REACT, COMBUSTION, ROTATING_FRAME);
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, LES), extras()); // LES+steady: nothing
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, RANS), incompSteady);
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, DNS), incompSteady);
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, NO), incompSteady);

    // Unsteady: also VOF, cavitation, moving mesh
    Set<ExtraModel> incompUnsteady =
        extras(VOF, CAVITATION, COMBUST_CHEM_REACT, COMBUSTION, ROTATING_FRAME, MOVING_MESH);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, LES), incompUnsteady);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, RANS), incompUnsteady);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, DNS), incompUnsteady);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, NO), incompUnsteady);

    // ── Compressible ────────────────────────────────────────────────────────
    // Steady LES: only radiation-family (no combustion-model by LES rules)
    m.put(new ModeKey(COMPRESSIBLE, STEADY, LES), extras(RADIATION, SURFACE_REACTION));

    // Steady RANS/DNS/NO: combustion + radiation + rotating
    Set<ExtraModel> compSteady = extras(COMBUST_CHEM_REACT, COMBUSTION, RADIATION, ROTATING_FRAME);
    m.put(new ModeKey(COMPRESSIBLE, STEADY, RANS), compSteady);
    m.put(new ModeKey(COMPRESSIBLE, STEADY, DNS), compSteady);
    m.put(new ModeKey(COMPRESSIBLE, STEADY, NO), compSteady);

    // Unsteady: also cavitation and moving mesh
    Set<ExtraModel> compUnsteady =
        extras(CAVITATION, COMBUST_CHEM_REACT, COMBUSTION, RADIATION, ROTATING_FRAME, MOVING_MESH);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, LES), compUnsteady);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, RANS), compUnsteady);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, DNS), compUnsteady);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, NO), compUnsteady);

    return Map.copyOf(m);
  }

  /**
   * Defines cross-compatibility between extra models.
   *
   * <p>For each extra {@code A}, the value is the set of extras (including {@code A} itself) that
   * remain selectable when {@code A} is selected. Selecting multiple extras restricts availability
   * to the intersection of their compatible sets.
   *
   * <p>Values are derived from the legacy {@code EXTRA_EXTRA} matrix.
   */
  private static Map<ExtraModel, Set<ExtraModel>> buildExtraCompatible() {
    Map<ExtraModel, Set<ExtraModel>> m = new EnumMap<>(ExtraModel.class);

    m.put(VOF, extras(VOF, ROTATING_FRAME));
    m.put(GHOST_FLUID, extras()); // selecting Ghost Fluid disables all others
    m.put(CAVITATION, extras(CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME, MOVING_MESH));
    m.put(
        COMBUST_CHEM_REACT,
        extras(
            CAVITATION,
            COMBUST_CHEM_REACT,
            RADIATION,
            SURFACE_REACTION,
            ROTATING_FRAME,
            MOVING_MESH));
    m.put(COMBUSTION, extras(CAVITATION, COMBUSTION, ROTATING_FRAME, MOVING_MESH));
    m.put(
        RADIATION,
        extras(
            VOF,
            CAVITATION,
            COMBUST_CHEM_REACT,
            RADIATION,
            SURFACE_REACTION,
            ROTATING_FRAME,
            MOVING_MESH));
    m.put(
        SURFACE_REACTION,
        extras(
            VOF,
            CAVITATION,
            COMBUST_CHEM_REACT,
            RADIATION,
            SURFACE_REACTION,
            ROTATING_FRAME,
            MOVING_MESH));
    m.put(PARTICLE_TRACK, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME, MOVING_MESH));
    m.put(POROUS_MODEL, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME, MOVING_MESH));
    m.put(ROTATING_FRAME, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME));
    m.put(MOVING_MESH, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, MOVING_MESH, OVERSET_GRID));
    m.put(OVERSET_GRID, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, MOVING_MESH));

    return Map.copyOf(m);
  }

  /** Convenience factory: creates an unmodifiable {@link EnumSet} from varargs. */
  @SafeVarargs
  private static Set<ExtraModel> extras(ExtraModel... items) {
    if (items.length == 0) return Set.of();
    Set<ExtraModel> set = EnumSet.noneOf(ExtraModel.class);
    for (ExtraModel e : items) set.add(e);
    return java.util.Collections.unmodifiableSet(set);
  }

  // ── Null-guard accessors ───────────────────────────────────────────────────

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
