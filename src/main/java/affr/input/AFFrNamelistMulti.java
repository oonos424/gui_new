package affr.input;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A multi-instance namelist — one that may appear multiple times in {@code fflow.ctl}, once per
 * domain entity (boundary surface, material region, probe point, species, etc.).
 *
 * <p>Examples: {@code &BOUNDARY}, {@code &INITIAL}, {@code &FLUID}, {@code &PROBE}, {@code
 * &SPECIES}, {@code &OUTPUT}.
 *
 * <p>Each instance is identified by the value of a designated <em>key variable</em>. For example, a
 * boundary block whose {@code boundary_name} field is {@code "inlet"} is stored under the key
 * {@code "inlet"}. The key variable name for each namelist is looked up in {@link #KEY_VARIABLE}.
 *
 * <p>Use {@link #addInstance(String)} to create a new instance and {@link #removeInstance(String)}
 * to delete one; both fire registered {@link AFFrNamelistListener}s.
 */
public final class AFFrNamelistMulti extends AFFrNamelist {

  /**
   * Maps each multi-instance namelist name (upper-case) to the field name of the variable that
   * identifies each instance.
   */
  public static final Map<String, String> KEY_VARIABLE =
      Map.ofEntries(
          Map.entry(NamelistNames.BOUNDARY, "boundary_name"),
          Map.entry(NamelistNames.INITIAL, "imat_u"),
          Map.entry(NamelistNames.FLUID, "imat_u"),
          Map.entry(NamelistNames.SOLID, "imat_u"),
          Map.entry(NamelistNames.OUTPUT, "file"),
          Map.entry(NamelistNames.MASS, "boundary_name"),
          Map.entry(NamelistNames.ENERGY, "boundary_name"),
          Map.entry(NamelistNames.PROBES, "label"),
          Map.entry(NamelistNames.FORCE_FLUIDS, "label"),
          Map.entry(NamelistNames.CDCL_OUTPUT, "label"),
          Map.entry(NamelistNames.SPECIES, "name"),
          Map.entry(NamelistNames.CHEMREAC, "name"),
          Map.entry(NamelistNames.VOF_INIT, "imat_u"),
          Map.entry(NamelistNames.CAVI_INIT, "imat_u"),
          Map.entry(NamelistNames.SURFACE_SPECIES, "name"),
          Map.entry(NamelistNames.SOUND_SOURCE, "label"),
          Map.entry(NamelistNames.SOUND_OBSERVER, "label"));

  /**
   * Maps each multi-instance namelist name (upper-case) to the prefix used when auto-generating new
   * instance names.
   */
  public static final Map<String, String> INSTANCE_PREFIX =
      Map.ofEntries(
          Map.entry(NamelistNames.BOUNDARY, "boundary_"),
          Map.entry(NamelistNames.INITIAL, "region_"),
          Map.entry(NamelistNames.FLUID, "fluid_"),
          Map.entry(NamelistNames.SOLID, "solid_"),
          Map.entry(NamelistNames.OUTPUT, "output_"),
          Map.entry(NamelistNames.MASS, "mass_"),
          Map.entry(NamelistNames.ENERGY, "energy_"),
          Map.entry(NamelistNames.PROBES, "probe_"),
          Map.entry(NamelistNames.FORCE_FLUIDS, "force_"),
          Map.entry(NamelistNames.CDCL_OUTPUT, "cdcl_"),
          Map.entry(NamelistNames.SPECIES, "species_"),
          Map.entry(NamelistNames.CHEMREAC, "reac_"),
          Map.entry(NamelistNames.VOF_INIT, "vofinit_"),
          Map.entry(NamelistNames.CAVI_INIT, "caviinit_"),
          Map.entry(NamelistNames.SURFACE_SPECIES, "surf_"),
          Map.entry(NamelistNames.SOUND_SOURCE, "source_"),
          Map.entry(NamelistNames.SOUND_OBSERVER, "observer_"));

  /**
   * The field name of the variable that identifies each instance (e.g. {@code "boundary_name"}).
   */
  private final String keyVariable;

  /**
   * Auto-naming prefix for new instances generated without an explicit key (e.g. {@code
   * "boundary_"}).
   */
  private final String instancePrefix;

  /**
   * Creates a multi-instance namelist with no initial instances.
   *
   * <p>The {@code keyVariable} and {@code instancePrefix} are resolved from the static tables. If
   * {@code listName} is not in the table, empty strings are used as fallbacks.
   *
   * @param listName upper-case namelist name (e.g. {@code "BOUNDARY"})
   */
  public AFFrNamelistMulti(String listName) {
    super(listName);
    this.keyVariable = KEY_VARIABLE.getOrDefault(listName, "");
    this.instancePrefix = INSTANCE_PREFIX.getOrDefault(listName, listName.toLowerCase() + "_");
  }

  /**
   * Creates a multi-instance namelist with explicitly supplied metadata.
   *
   * <p>Use this constructor for namelist names not covered by the built-in tables.
   *
   * @param listName upper-case namelist name
   * @param keyVariable field name of the identifying variable within each instance
   * @param instancePrefix auto-naming prefix for new instances
   */
  public AFFrNamelistMulti(String listName, String keyVariable, String instancePrefix) {
    super(listName);
    this.keyVariable = keyVariable;
    this.instancePrefix = instancePrefix;
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  /**
   * The field name of the variable that identifies each instance (e.g. {@code "boundary_name"}).
   */
  public String getKeyVariable() {
    return keyVariable;
  }

  /** The prefix used when auto-generating instance names (e.g. {@code "boundary_"}). */
  public String getInstancePrefix() {
    return instancePrefix;
  }

  // ── Instance management ───────────────────────────────────────────────────

  /**
   * Creates a new instance keyed by {@code instanceKey} and fires all registered {@link
   * AFFrNamelistListener}s.
   *
   * <p>If an instance with the given key already exists, this method is a no-op (the existing
   * instance is not replaced).
   *
   * @param instanceKey the key for the new instance (e.g. {@code "inlet"})
   * @return {@code true} if a new instance was created; {@code false} if the key was already
   *     present
   */
  public boolean addInstance(String instanceKey) {
    if (nmlistData.containsKey(instanceKey)) {
      return false;
    }
    nmlistData.put(instanceKey, new AFFrNamelistData(listName, instanceKey));
    fireNamelistListeners();
    return true;
  }

  /**
   * Removes the instance keyed by {@code instanceKey} and fires all registered {@link
   * AFFrNamelistListener}s.
   *
   * <p>If no such instance exists, this method is a no-op.
   *
   * @param instanceKey the key of the instance to remove
   * @return {@code true} if the instance was removed; {@code false} if it did not exist
   */
  public boolean removeInstance(String instanceKey) {
    if (nmlistData.remove(instanceKey) == null) {
      return false;
    }
    fireNamelistListeners();
    return true;
  }

  /**
   * Generates a new unique instance key using the {@link #instancePrefix} and the next available
   * integer suffix.
   *
   * <p>The suffix is one more than the highest existing numeric suffix for this prefix. Returns
   * {@code prefix + "01"} when no existing instances match the prefix pattern.
   *
   * @return a key not currently present in this namelist (e.g. {@code "boundary_03"})
   */
  public String nextInstanceKey() {
    Set<String> keys = nmlistData.keySet();
    int max =
        keys.stream()
            .filter(k -> k.startsWith(instancePrefix))
            .mapToInt(
                k -> {
                  try {
                    return Integer.parseInt(k.substring(instancePrefix.length()));
                  } catch (NumberFormatException e) {
                    return 0;
                  }
                })
            .max()
            .orElse(0);
    return String.format("%s%02d", instancePrefix, max + 1);
  }

  /**
   * Removes all instances and fires one structure-listener notification per removed instance.
   *
   * <p>Listener registrations on the namelist itself are preserved. Used by {@link
   * AFFrInput#reload(java.nio.file.Path)} to reset the instance set before re-parsing.
   */
  public void clearInstances() {
    List<String> keys = List.copyOf(nmlistData.keySet());
    for (String key : keys) {
      nmlistData.remove(key);
      fireNamelistListeners();
    }
  }

  /**
   * Returns an ordered snapshot of all current instance keys. The order reflects insertion order
   * (iteration order of the underlying {@link java.util.HashMap} is not guaranteed; callers that
   * need stable ordering should sort externally).
   */
  public List<String> getInstanceKeyList() {
    return List.copyOf(nmlistData.keySet());
  }
}
