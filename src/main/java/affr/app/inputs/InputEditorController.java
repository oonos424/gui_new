package affr.app.inputs;

import affr.app.LanguageMenu;
import affr.fx.viewmodel.inputs.InputTab;
import affr.project.AFFrCalculation;
import affr.project.AFFrCalculationModel;
import affr.util.i18n.I18n;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * FXML controller for the Input Editor shell — the persistent chrome around per-tab CFD setting
 * forms.
 *
 * <p>Owns the header navigation/action buttons and the MANAGER, which is laid out as a column of
 * "tab" {@link ToggleButton}s on the left ({@link #tabButtons}) plus a {@link StackPane} ({@link
 * #tabContent}) that swaps in the active tab's form. The set of tabs depends on the calculation's
 * {@link AFFrCalculationModel}; visibility logic lives in {@link
 * InputTab#tabsFor(AFFrCalculationModel)}.
 *
 * <p>This deliberately does not use {@link javafx.scene.control.TabPane} with {@code side="LEFT"}
 * because that would force tab labels to be rotated -90° by {@code TabPaneSkin} (applied via
 * Java-level {@code setRotate}, not from CSS, and therefore not overridable from a stylesheet). A
 * {@link VBox} of {@link ToggleButton}s gives horizontal-text tabs running down the left edge with
 * full visual control.
 *
 * <p>Tab content is intentionally empty in this phase — the per-tab forms are driven by the solver
 * input data model, which is not yet implemented. Each tab therefore receives an empty {@link Pane}
 * as its content.
 *
 * <p>Lifecycle: FXML loading injects the widgets, {@link #initialize()} sets locale-independent
 * defaults, then the caller invokes {@link #init(AFFrCalculation, Runnable, Runnable)} to bind a
 * specific calculation and wire navigation.
 */
public final class InputEditorController {

  // ── FXML-injected widgets ──────────────────────────────────────────────────

  @FXML private @Nullable BorderPane rootPane;
  @FXML private @Nullable SplitPane splitPane;
  @FXML private @Nullable HBox manager;
  @FXML private @Nullable VBox tabButtons;
  @FXML private @Nullable StackPane tabContent;
  @FXML private @Nullable StackPane viewerPane;
  @FXML private @Nullable Label viewerPlaceholder;

  // Header — left
  @FXML private @Nullable Button backButton;
  @FXML private @Nullable Button homeButton;

  // Header — right (action buttons; placeholders, no actions wired in this phase)
  @FXML private @Nullable Button changeSettingsButton;
  @FXML private @Nullable Button runButton;
  @FXML private @Nullable Button saveButton;
  @FXML private @Nullable Button editSubroutineButton;
  @FXML private @Nullable Button confirmSettingsButton;
  @FXML private @Nullable MenuButton menuButton;

  // Header — right menu items (Setting / About are placeholders mirroring the shell menu)
  @FXML private @Nullable MenuItem settingMenuItem;
  @FXML private @Nullable MenuItem aboutMenuItem;
  @FXML private @Nullable Menu languageMenu;
  @FXML private @Nullable RadioMenuItem langEnItem;
  @FXML private @Nullable RadioMenuItem langJaItem;

  // ── State (set by init()) ─────────────────────────────────────────────────

  private @Nullable AFFrCalculation calculation;
  private @Nullable Runnable onBack;
  private @Nullable Runnable onHome;

  /** Group ensuring exactly one tab toggle is selected at a time. */
  private final ToggleGroup tabGroup = new ToggleGroup();

  /**
   * Per-tab content panes — keyed by {@link InputTab} so {@link #refreshTabLabels()} can update the
   * correct toggle button on locale change. Insertion order matches the display order.
   */
  private final Map<InputTab, ToggleButton> tabButtonByInputTab = new LinkedHashMap<>();

  private final Map<InputTab, Pane> tabPaneByInputTab = new LinkedHashMap<>();

  // ── FXML lifecycle ────────────────────────────────────────────────────────

  @FXML
  private void initialize() {
    applyLabels();
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Wires the editor to a specific calculation and to navigation callbacks.
   *
   * @param calculation the calculation being edited; its {@link AFFrCalculationModel} drives which
   *     tabs are shown
   * @param onBack invoked when the user clicks the Back button — return to the Calculation List
   * @param onHome invoked when the user clicks the Home button — return to the Project Browser
   */
  public void init(AFFrCalculation calculation, Runnable onBack, Runnable onHome) {
    this.calculation = calculation;
    this.onBack = onBack;
    this.onHome = onHome;

    requireBackButton().setOnAction(e -> onBack.run());
    requireHomeButton().setOnAction(e -> onHome.run());

    LanguageMenu.install(requireLangEnItem(), requireLangJaItem());

    populateTabs(calculation.getModel());

    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              applyLabels();
              refreshTabLabels();
            });
  }

  // ── Tabs ──────────────────────────────────────────────────────────────────

  /**
   * Rebuilds the tab column from scratch using {@link InputTab#tabsFor(AFFrCalculationModel)}.
   *
   * <p>For each {@link InputTab}, creates a {@link ToggleButton} (added to {@link #tabButtons} and
   * to {@link #tabGroup}) and an empty content {@link Pane}. Selecting a toggle swaps the
   * corresponding pane into {@link #tabContent}. The first tab is selected by default.
   *
   * <p>Tab content is an empty {@link Pane} for now; per-tab forms are deferred until the solver
   * input model is implemented.
   */
  private void populateTabs(AFFrCalculationModel model) {
    VBox column = requireTabButtons();
    column.getChildren().clear();
    tabButtonByInputTab.clear();
    tabPaneByInputTab.clear();

    @Nullable ToggleButton firstButton = null;
    for (InputTab inputTab : InputTab.tabsFor(model)) {
      ToggleButton button = new ToggleButton(I18n.get(inputTab.labelKey()));
      button.getStyleClass().add("input-editor-tab-button");
      button.setMaxWidth(Double.MAX_VALUE);
      button.setUserData(inputTab);
      button.setToggleGroup(tabGroup);

      Pane content = new Pane();
      content.getStyleClass().add("input-editor-tab-pane");
      tabPaneByInputTab.put(inputTab, content);

      button
          .selectedProperty()
          .addListener(
              (obs, old, selected) -> {
                if (Boolean.TRUE.equals(selected)) {
                  requireTabContent().getChildren().setAll(content);
                }
              });

      column.getChildren().add(button);
      tabButtonByInputTab.put(inputTab, button);
      if (firstButton == null) firstButton = button;
    }

    // Disallow toggling the active tab off (clicking a selected ToggleButton would otherwise
    // deselect it, leaving zero tabs visible).
    tabGroup
        .selectedToggleProperty()
        .addListener(
            (obs, old, current) -> {
              if (current == null && old != null) {
                old.setSelected(true);
              }
            });

    if (firstButton != null) {
      firstButton.setSelected(true);
    } else {
      requireTabContent().getChildren().clear();
    }
  }

  /**
   * Returns an unmodifiable view of the tab → toggle-button map in display order. Package-private
   * for tests; production code should not depend on this map directly.
   */
  Map<InputTab, ToggleButton> tabButtonsByInputTab() {
    return Collections.unmodifiableMap(tabButtonByInputTab);
  }

  /** Package-private accessor for the menu button (used by tests to inspect menu items). */
  MenuButton menuButtonNode() {
    return requireMenuButton();
  }

  /** Package-private accessor for the English language radio item (used by tests). */
  RadioMenuItem langEnItemNode() {
    return requireLangEnItem();
  }

  /** Package-private accessor for the Japanese language radio item (used by tests). */
  RadioMenuItem langJaItemNode() {
    return requireLangJaItem();
  }

  /** Re-resolves every tab button's label after a locale change. */
  private void refreshTabLabels() {
    for (Map.Entry<InputTab, ToggleButton> entry : tabButtonByInputTab.entrySet()) {
      entry.getValue().setText(I18n.get(entry.getKey().labelKey()));
    }
  }

  // ── Locale-driven label refresh for header buttons ────────────────────────

  private void applyLabels() {
    requireBackButton().setText(I18n.get("inputEditor.header.back"));
    requireHomeButton().setText(I18n.get("inputEditor.header.home"));
    requireChangeSettingsButton().setText(I18n.get("inputEditor.header.changeSettings"));
    requireRunButton().setText(I18n.get("inputEditor.header.run"));
    requireSaveButton().setText(I18n.get("inputEditor.header.save"));
    requireEditSubroutineButton().setText(I18n.get("inputEditor.header.editSubroutine"));
    requireConfirmSettingsButton().setText(I18n.get("inputEditor.header.confirmSettings"));
    requireMenuButton().setText(I18n.get("inputEditor.header.menu"));
    requireSettingMenuItem().setText(I18n.get("inputEditor.menu.setting"));
    requireAboutMenuItem().setText(I18n.get("inputEditor.menu.about"));
    requireLanguageMenu().setText(I18n.get("inputEditor.menu.language"));
  }

  // ── Null-guard helpers ────────────────────────────────────────────────────

  private VBox requireTabButtons() {
    VBox v = tabButtons;
    if (v == null) throw new IllegalStateException("tabButtons not injected");
    return v;
  }

  private StackPane requireTabContent() {
    StackPane s = tabContent;
    if (s == null) throw new IllegalStateException("tabContent not injected");
    return s;
  }

  private Button requireBackButton() {
    Button b = backButton;
    if (b == null) throw new IllegalStateException("backButton not injected");
    return b;
  }

  private Button requireHomeButton() {
    Button b = homeButton;
    if (b == null) throw new IllegalStateException("homeButton not injected");
    return b;
  }

  private Button requireChangeSettingsButton() {
    Button b = changeSettingsButton;
    if (b == null) throw new IllegalStateException("changeSettingsButton not injected");
    return b;
  }

  private Button requireRunButton() {
    Button b = runButton;
    if (b == null) throw new IllegalStateException("runButton not injected");
    return b;
  }

  private Button requireSaveButton() {
    Button b = saveButton;
    if (b == null) throw new IllegalStateException("saveButton not injected");
    return b;
  }

  private Button requireEditSubroutineButton() {
    Button b = editSubroutineButton;
    if (b == null) throw new IllegalStateException("editSubroutineButton not injected");
    return b;
  }

  private Button requireConfirmSettingsButton() {
    Button b = confirmSettingsButton;
    if (b == null) throw new IllegalStateException("confirmSettingsButton not injected");
    return b;
  }

  private MenuButton requireMenuButton() {
    MenuButton b = menuButton;
    if (b == null) throw new IllegalStateException("menuButton not injected");
    return b;
  }

  private MenuItem requireSettingMenuItem() {
    MenuItem m = settingMenuItem;
    if (m == null) throw new IllegalStateException("settingMenuItem not injected");
    return m;
  }

  private MenuItem requireAboutMenuItem() {
    MenuItem m = aboutMenuItem;
    if (m == null) throw new IllegalStateException("aboutMenuItem not injected");
    return m;
  }

  private Menu requireLanguageMenu() {
    Menu m = languageMenu;
    if (m == null) throw new IllegalStateException("languageMenu not injected");
    return m;
  }

  private RadioMenuItem requireLangEnItem() {
    RadioMenuItem r = langEnItem;
    if (r == null) throw new IllegalStateException("langEnItem not injected");
    return r;
  }

  private RadioMenuItem requireLangJaItem() {
    RadioMenuItem r = langJaItem;
    if (r == null) throw new IllegalStateException("langJaItem not injected");
    return r;
  }
}
