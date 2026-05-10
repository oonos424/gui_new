package affr.input;

import affr.project.AFFrCalculationModel;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The in-memory representation of all solver settings for one calculation.
 *
 * <p>There is exactly one {@code AFFrInput} per open calculation. It is never replaced — it is
 * updated in place as the user edits fields. The UI binds to the namelist objects inside this root;
 * the root itself does not change after construction.
 *
 * <p>Namelists are divided into two maps:
 *
 * <ul>
 *   <li>{@link #nmlistSingles} — the 47 namelists that appear at most once in {@code fflow.ctl}
 *       (e.g. {@code &MODEL}, {@code &TIME}, {@code &FLAGS}).
 *   <li>{@link #nmlistMultis} — the 17 namelists that may appear multiple times, one block per
 *       domain entity (e.g. {@code &BOUNDARY}, {@code &INITIAL}, {@code &PROBES}).
 * </ul>
 *
 * <p>Namelists are populated by a loader (not yet implemented). The {@link #linesOfInput} snapshot
 * is the raw file content from the last read and is used only for reload; it is not consulted
 * during editing.
 */
public final class AFFrInput {

  // ── Standard single-instance namelist names ────────────────────────────────

  /** All 30 standard single-instance namelist names (upper-case). */
  public static final List<String> SINGLE_NAMELIST_NAMES =
      List.of(
          NamelistNames.FILES,
          NamelistNames.MODEL,
          NamelistNames.LES,
          NamelistNames.TURBPARM,
          NamelistNames.GRAVITY,
          NamelistNames.HPC,
          NamelistNames.TIME,
          NamelistNames.DELTAT,
          NamelistNames.FLAGS,
          NamelistNames.SIMPLE,
          NamelistNames.CGSOLVER,
          NamelistNames.GUI,
          NamelistNames.FLAMELET,
          NamelistNames.FLAMELET_FUNC,
          NamelistNames.CAVITATION,
          NamelistNames.VOF,
          NamelistNames.CHEMCNTL,
          NamelistNames.USRSUB,
          NamelistNames.KEMODEL,
          NamelistNames.KOMGMODEL,
          NamelistNames.PARTICLE_MODEL,
          NamelistNames.INJECTOR_NUMBER,
          NamelistNames.SOUND,
          NamelistNames.EUL2PH,
          NamelistNames.HPC_CNTL,
          NamelistNames.MERGE_OR_REDECOMPOSITION,
          NamelistNames.HPC_MERGE,
          NamelistNames.HPC_REDECOMP,
          NamelistNames.SIZES,
          NamelistNames.RADOPTION);

  /** All 17 standard multi-instance namelist names (upper-case). */
  public static final List<String> MULTI_NAMELIST_NAMES =
      List.of(
          NamelistNames.BOUNDARY,
          NamelistNames.FLUID,
          NamelistNames.SOLID,
          NamelistNames.INITIAL,
          NamelistNames.OUTPUT,
          NamelistNames.MASS,
          NamelistNames.ENERGY,
          NamelistNames.SOUND_SOURCE,
          NamelistNames.SOUND_OBSERVER,
          NamelistNames.PROBES,
          NamelistNames.FORCE_FLUIDS,
          NamelistNames.CDCL_OUTPUT,
          NamelistNames.SPECIES,
          NamelistNames.CHEMREAC,
          NamelistNames.VOF_INIT,
          NamelistNames.CAVI_INIT,
          NamelistNames.SURFACE_SPECIES);

  // ── Fields ────────────────────────────────────────────────────────────────

  private final Map<String, AFFrNamelistSingle> nmlistSingles;
  private final Map<String, AFFrNamelistMulti> nmlistMultis;
  private final AFFrCalculationModel model;
  private final List<String> linesOfInput;

  /**
   * Creates an {@code AFFrInput} with the given namelist maps and metadata.
   *
   * <p>Both maps are copied defensively. The caller retains no reference that could mutate the
   * internal maps after construction.
   *
   * @param nmlistSingles single-instance namelists keyed by upper-case name
   * @param nmlistMultis multi-instance namelists keyed by upper-case name
   * @param model the physics model selection that governs which namelists are active
   * @param linesOfInput raw file lines from the last read; empty when created from defaults
   */
  public AFFrInput(
      Map<String, AFFrNamelistSingle> nmlistSingles,
      Map<String, AFFrNamelistMulti> nmlistMultis,
      AFFrCalculationModel model,
      List<String> linesOfInput) {
    this.nmlistSingles = Collections.unmodifiableMap(new HashMap<>(nmlistSingles));
    this.nmlistMultis = Collections.unmodifiableMap(new HashMap<>(nmlistMultis));
    this.model = model;
    this.linesOfInput = List.copyOf(linesOfInput);
  }

  // ── Factories ─────────────────────────────────────────────────────────────

  /**
   * Creates an {@code AFFrInput} pre-populated with all standard namelists (singles and multis) but
   * with no field values set. This is the starting point for a new calculation with no existing
   * {@code fflow.ctl}.
   *
   * @param model the physics model selection for the new calculation
   * @return a fresh, empty {@code AFFrInput}
   */
  public static AFFrInput createEmpty(AFFrCalculationModel model) {
    Map<String, AFFrNamelistSingle> singles = new HashMap<>();
    for (String name : SINGLE_NAMELIST_NAMES) {
      singles.put(name, new AFFrNamelistSingle(name));
    }
    Map<String, AFFrNamelistMulti> multis = new HashMap<>();
    for (String name : MULTI_NAMELIST_NAMES) {
      multis.put(name, new AFFrNamelistMulti(name));
    }
    return new AFFrInput(singles, multis, model, List.of());
  }

  /**
   * Reads {@code ctlFile} and returns a fully populated {@code AFFrInput}.
   *
   * <p>Initialization sequence:
   *
   * <ol>
   *   <li>Build empty namelist maps.
   *   <li>Parse the file via {@link CtlParser#populate}.
   *   <li>Translate to GUI format (no-op stub — translation rules are added per-field when
   *       required).
   *   <li>Fire all value listeners so that any already-registered binding components receive the
   *       initial values.
   * </ol>
   *
   * <p>The file is read as UTF-8. If that fails with a {@link MalformedInputException} (e.g. the
   * solver wrote the file using MS932 on Japanese Windows), reading is retried with the JVM default
   * charset.
   *
   * @param ctlFile path to the {@code fflow.ctl} file
   * @param model the physics model selection for this calculation
   * @return the populated model
   * @throws IOException if the file cannot be read
   */
  public static AFFrInput loadFromFile(Path ctlFile, AFFrCalculationModel model)
      throws IOException {
    String content = readContent(ctlFile);
    List<String> lines = List.of(content.split("\n", -1));

    Map<String, AFFrNamelistSingle> singles = new HashMap<>();
    for (String name : SINGLE_NAMELIST_NAMES) {
      singles.put(name, new AFFrNamelistSingle(name));
    }
    Map<String, AFFrNamelistMulti> multis = new HashMap<>();
    for (String name : MULTI_NAMELIST_NAMES) {
      multis.put(name, new AFFrNamelistMulti(name));
    }

    AFFrInput input = new AFFrInput(singles, multis, model, lines);

    // Step 2: load values
    CtlParser.populate(input, content);

    // Step 3: translate to GUI format (no-op stub — add per-field rules here when needed)

    // Step 5: fire all notifications
    input.fireAllValueListeners();

    return input;
  }

  // ── Namelist accessors ────────────────────────────────────────────────────

  /**
   * Returns the single-instance namelist map (upper-case name → namelist). The returned map is
   * unmodifiable.
   */
  public Map<String, AFFrNamelistSingle> getNmlistSingles() {
    return nmlistSingles;
  }

  /**
   * Returns the multi-instance namelist map (upper-case name → namelist). The returned map is
   * unmodifiable.
   */
  public Map<String, AFFrNamelistMulti> getNmlistMultis() {
    return nmlistMultis;
  }

  /**
   * Returns the single-instance namelist with the given upper-case name, or {@code null} if it is
   * not present.
   *
   * @param name upper-case namelist name (e.g. {@code "MODEL"})
   */
  public @Nullable AFFrNamelistSingle getSingle(String name) {
    return nmlistSingles.get(name);
  }

  /**
   * Returns the multi-instance namelist with the given upper-case name, or {@code null} if it is
   * not present.
   *
   * @param name upper-case namelist name (e.g. {@code "BOUNDARY"})
   */
  public @Nullable AFFrNamelistMulti getMulti(String name) {
    return nmlistMultis.get(name);
  }

  /**
   * Returns the namelist (single or multi) with the given upper-case name, or {@code null} if
   * neither map contains it.
   *
   * @param name upper-case namelist name
   */
  public @Nullable AFFrNamelist getNamelist(String name) {
    AFFrNamelist single = nmlistSingles.get(name);
    if (single != null) return single;
    return nmlistMultis.get(name);
  }

  // ── Metadata ──────────────────────────────────────────────────────────────

  /**
   * The physics model selection that was active when this input was constructed. Determines which
   * namelists are relevant and which form panels are shown.
   */
  public AFFrCalculationModel getModel() {
    return model;
  }

  /**
   * The raw file lines from the last read of {@code fflow.ctl}. This list is immutable and is not
   * used during editing — it is retained only as the source for a future re-parse on reload. Empty
   * when the input was created from defaults.
   */
  public List<String> getLinesOfInput() {
    return linesOfInput;
  }

  // ── Bulk reload support ───────────────────────────────────────────────────

  /**
   * Clears all field values in every namelist instance. Conditional field registrations and
   * listener registrations are preserved. Called at the start of a model reload, before re-parsing
   * the file.
   *
   * <p>After clearing and re-loading values, call {@link #fireAllValueListeners()} to push the new
   * state to all bound widgets.
   */
  public void clearAllValues() {
    for (AFFrNamelistSingle s : nmlistSingles.values()) {
      s.clearAllValues();
    }
    for (AFFrNamelistMulti m : nmlistMultis.values()) {
      m.clearAllValues();
    }
  }

  /**
   * Fires all registered value listeners across every namelist and every instance. Use this after a
   * bulk reload to cause every bound widget to refresh from the new model state.
   */
  public void fireAllValueListeners() {
    for (AFFrNamelistSingle s : nmlistSingles.values()) {
      s.fireAllValueListeners();
    }
    for (AFFrNamelistMulti m : nmlistMultis.values()) {
      m.fireAllValueListeners();
    }
  }

  /**
   * Reloads this model in-place from {@code ctlFile}.
   *
   * <p>Listener registrations are preserved throughout — bound widgets remain attached and
   * automatically refresh when the final {@link #fireAllValueListeners()} call runs.
   *
   * <p>Reload sequence:
   *
   * <ol>
   *   <li>Clear all field values from every namelist instance.
   *   <li>Clear all instances from every multi-instance namelist (fires structure listeners).
   *   <li>Re-parse the file via {@link CtlParser#populate}.
   *   <li>Translate to GUI format (no-op stub).
   *   <li>Fire all value listeners.
   * </ol>
   *
   * @param ctlFile path to the {@code fflow.ctl} file to reload
   * @throws IOException if the file cannot be read
   */
  public void reload(Path ctlFile) throws IOException {
    String content = readContent(ctlFile);

    // Step 1: clear values
    clearAllValues();

    // Step 2: clear multi-namelist instances
    for (AFFrNamelistMulti m : nmlistMultis.values()) {
      m.clearInstances();
    }

    // Step 3: re-parse
    CtlParser.populate(this, content);

    // Step 4: translate to GUI format (no-op stub)

    // Step 5: fire all notifications
    fireAllValueListeners();
  }

  // ── I/O helpers ───────────────────────────────────────────────────────────

  /**
   * Reads {@code path} as UTF-8, retrying with the JVM default charset on {@link
   * MalformedInputException}.
   */
  private static String readContent(Path path) throws IOException {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (MalformedInputException e) {
      return Files.readString(path, Charset.defaultCharset());
    }
  }
}
