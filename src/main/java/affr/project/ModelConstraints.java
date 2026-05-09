package affr.project;

import static affr.project.ComprsModel.COMPRESSIBLE;
import static affr.project.ComprsModel.INCOMPRESSIBLE;
import static affr.project.ExtraModel.CAVITATION;
import static affr.project.ExtraModel.COMBUSTION;
import static affr.project.ExtraModel.COMBUST_CHEM_REACT;
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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Domain rules for which {@link ExtraModel} add-ons are compatible with a given base model
 * selection and with each other.
 *
 * <p>This class is a pure-Java domain service with no JavaFX or UI dependency. It can be queried
 * from any layer and from any thread.
 *
 * <p>Two constraint tables drive availability:
 *
 * <ol>
 *   <li>{@link #BASIC_ALLOWED} — for each (comprs, steady, turb) triple, the extras that are
 *       fundamentally compatible.
 *   <li>{@link #EXTRA_COMPATIBLE} — for each extra, the set of extras (including itself) that
 *       remain selectable when that extra is chosen. Selecting multiple extras restricts the
 *       allowed set to the intersection of all their compatibility sets.
 * </ol>
 *
 * <p>Use {@link #allowedExtras} to compute the full set for a given state in one call.
 */
public final class ModelConstraints {

  private ModelConstraints() {}

  // ── Constraint tables ─────────────────────────────────────────────────────

  private record ModeKey(ComprsModel comprs, SteadyModel steady, TurbModel turb) {}

  /**
   * For each (comprs, steady, turb) combination, the extras that may be enabled at all. Missing
   * keys default to an empty set.
   */
  public static final Map<ModeKey, Set<ExtraModel>> BASIC_ALLOWED = buildBasicAllowed();

  /**
   * For each extra model, the set of extras (including itself) that remain selectable when that
   * extra is chosen. Missing keys default to an empty set.
   */
  public static final Map<ExtraModel, Set<ExtraModel>> EXTRA_COMPATIBLE = buildExtraCompatible();

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Returns the set of {@link ExtraModel}s that may be enabled given the current base model
   * selection and the set of extras already selected.
   *
   * <p>Algorithm:
   *
   * <ol>
   *   <li>Start with the base-allowed set for (comprs, steady, turb).
   *   <li>Intersect with the compatibility set of each currently-selected extra.
   * </ol>
   *
   * @param comprs current compressibility selection
   * @param steady current steady/unsteady selection
   * @param turb current turbulence selection
   * @param selected the extras currently selected by the user
   * @return unmodifiable set of extras that are permitted to be enabled
   */
  public static Set<ExtraModel> allowedExtras(
      ComprsModel comprs, SteadyModel steady, TurbModel turb, Set<ExtraModel> selected) {
    ModeKey key = new ModeKey(comprs, steady, turb);
    Set<ExtraModel> basicSet = BASIC_ALLOWED.getOrDefault(key, Set.of());
    Set<ExtraModel> allowed =
        basicSet.isEmpty() ? EnumSet.noneOf(ExtraModel.class) : EnumSet.copyOf(basicSet);
    for (ExtraModel extra : selected) {
      allowed.retainAll(EXTRA_COMPATIBLE.getOrDefault(extra, Set.of()));
    }
    return Set.copyOf(allowed);
  }

  // ── Table builders ────────────────────────────────────────────────────────

  private static Map<ModeKey, Set<ExtraModel>> buildBasicAllowed() {
    Map<ModeKey, Set<ExtraModel>> m = new HashMap<>();

    // ── Incompressible ──────────────────────────────────────────────────────
    Set<ExtraModel> incompSteady = extras(COMBUST_CHEM_REACT, COMBUSTION, ROTATING_FRAME);
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, LES), extras());
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, RANS), incompSteady);
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, DNS), incompSteady);
    m.put(new ModeKey(INCOMPRESSIBLE, STEADY, NO), incompSteady);

    Set<ExtraModel> incompUnsteady =
        extras(VOF, CAVITATION, COMBUST_CHEM_REACT, COMBUSTION, ROTATING_FRAME, MOVING_MESH);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, LES), incompUnsteady);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, RANS), incompUnsteady);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, DNS), incompUnsteady);
    m.put(new ModeKey(INCOMPRESSIBLE, UNSTEADY, NO), incompUnsteady);

    // ── Compressible ────────────────────────────────────────────────────────
    m.put(new ModeKey(COMPRESSIBLE, STEADY, LES), extras(RADIATION, SURFACE_REACTION));

    Set<ExtraModel> compSteady = extras(COMBUST_CHEM_REACT, COMBUSTION, RADIATION, ROTATING_FRAME);
    m.put(new ModeKey(COMPRESSIBLE, STEADY, RANS), compSteady);
    m.put(new ModeKey(COMPRESSIBLE, STEADY, DNS), compSteady);
    m.put(new ModeKey(COMPRESSIBLE, STEADY, NO), compSteady);

    Set<ExtraModel> compUnsteady =
        extras(CAVITATION, COMBUST_CHEM_REACT, COMBUSTION, RADIATION, ROTATING_FRAME, MOVING_MESH);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, LES), compUnsteady);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, RANS), compUnsteady);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, DNS), compUnsteady);
    m.put(new ModeKey(COMPRESSIBLE, UNSTEADY, NO), compUnsteady);

    return Map.copyOf(m);
  }

  private static Map<ExtraModel, Set<ExtraModel>> buildExtraCompatible() {
    Map<ExtraModel, Set<ExtraModel>> m = new EnumMap<>(ExtraModel.class);

    m.put(VOF, extras(VOF, ROTATING_FRAME));
    m.put(ExtraModel.GHOST_FLUID, extras(ExtraModel.GHOST_FLUID));
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
    m.put(
        PARTICLE_TRACK,
        extras(PARTICLE_TRACK, VOF, CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME, MOVING_MESH));
    m.put(
        POROUS_MODEL,
        extras(POROUS_MODEL, VOF, CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME, MOVING_MESH));
    m.put(ROTATING_FRAME, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, ROTATING_FRAME));
    m.put(MOVING_MESH, extras(VOF, CAVITATION, COMBUST_CHEM_REACT, MOVING_MESH, OVERSET_GRID));
    m.put(OVERSET_GRID, extras(OVERSET_GRID, VOF, CAVITATION, COMBUST_CHEM_REACT, MOVING_MESH));

    return Map.copyOf(m);
  }

  @SafeVarargs
  private static Set<ExtraModel> extras(ExtraModel... items) {
    if (items.length == 0) return Set.of();
    Set<ExtraModel> set = EnumSet.noneOf(ExtraModel.class);
    for (ExtraModel e : items) set.add(e);
    return Set.copyOf(set);
  }
}
