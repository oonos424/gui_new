package affr.fx.viewmodel.inputs;

import affr.project.AFFrCalculationModel;
import affr.project.ExtraModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Catalog of tabs that the Input Editor can display, plus the rule for deciding which ones to show
 * for a given {@link AFFrCalculationModel}.
 *
 * <p>The set of tabs depends on the calculation's high-level model selection:
 *
 * <ul>
 *   <li>The six "standard" tabs are always shown, in the order declared on this enum.
 *   <li>The "optional" tabs are shown only when the calculation has the corresponding {@link
 *       ExtraModel}(s) enabled. Some optional tabs respond to more than one {@link ExtraModel}.
 * </ul>
 *
 * <p>The mapping mirrors the rules in {@code ui_spec/03_input_editor/implementation.md}.
 */
public enum InputTab {

  // ── Standard tabs (always shown, in this order) ───────────────────────────
  MESH("inputEditor.tab.mesh"),
  MODEL("inputEditor.tab.model"),
  FLUID("inputEditor.tab.fluid"),
  BOUNDARY("inputEditor.tab.boundary"),
  SCHEME("inputEditor.tab.scheme"),
  IO_MONITORING("inputEditor.tab.ioMonitoring"),

  // ── Optional tabs (shown only when ExtraModel(s) trigger them) ────────────
  COMBUSTION("inputEditor.tab.combustion"),
  PARTICLE_TRACK("inputEditor.tab.particleTrack"),
  RADIATION("inputEditor.tab.radiation"),
  REACTION("inputEditor.tab.reaction"),
  CAVITATION("inputEditor.tab.cavitation"),
  VOF("inputEditor.tab.vof");

  private final String labelKey;

  InputTab(String labelKey) {
    this.labelKey = labelKey;
  }

  /** i18n message key for this tab's label. Resolve via {@code I18n.get(labelKey())}. */
  public String labelKey() {
    return labelKey;
  }

  /**
   * Returns the tabs to show for the given model, in display order: the six standard tabs first,
   * then any optional tabs whose {@link ExtraModel} triggers are present.
   */
  public static List<InputTab> tabsFor(AFFrCalculationModel model) {
    List<InputTab> tabs = new ArrayList<>(8);

    tabs.add(MESH);
    tabs.add(MODEL);
    tabs.add(FLUID);
    tabs.add(BOUNDARY);
    tabs.add(SCHEME);
    tabs.add(IO_MONITORING);

    Set<ExtraModel> extras = model.extraModelSet();

    if (extras.contains(ExtraModel.COMBUSTION) || extras.contains(ExtraModel.COMBUST_CHEM_REACT)) {
      tabs.add(COMBUSTION);
    }
    if (extras.contains(ExtraModel.PARTICLE_TRACK)) {
      tabs.add(PARTICLE_TRACK);
    }
    if (extras.contains(ExtraModel.RADIATION)) {
      tabs.add(RADIATION);
    }
    if (extras.contains(ExtraModel.COMBUST_CHEM_REACT)) {
      tabs.add(REACTION);
    }
    if (extras.contains(ExtraModel.CAVITATION)) {
      tabs.add(CAVITATION);
    }
    if (extras.contains(ExtraModel.VOF) || extras.contains(ExtraModel.GHOST_FLUID)) {
      tabs.add(VOF);
    }

    return tabs;
  }
}
