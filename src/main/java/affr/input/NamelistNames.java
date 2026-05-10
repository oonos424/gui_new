package affr.input;

/**
 * String constants for every solver namelist name that appears in {@code fflow.ctl}.
 *
 * <p>Use these constants wherever a namelist name is needed (e.g. calls to {@link
 * AFFrInput#getSingle}, {@link AFFrInput#getMulti}, {@link AFFrInput#getNamelist}, or {@link
 * AFFrValueCondition} constructor arguments) instead of string literals, so that references are
 * refactor-safe and autocomplete-assisted.
 *
 * <p>All constants are upper-case, matching the convention used throughout the model.
 */
public final class NamelistNames {

  private NamelistNames() {}

  // ── Single-instance namelists ──────────────────────────────────────────────

  public static final String FILES = "FILES";
  public static final String MODEL = "MODEL";
  public static final String LES = "LES";
  public static final String TURBPARM = "TURBPARM";
  public static final String GRAVITY = "GRAVITY";
  public static final String HPC = "HPC";
  public static final String TIME = "TIME";
  public static final String DELTAT = "DELTAT";
  public static final String FLAGS = "FLAGS";
  public static final String SIMPLE = "SIMPLE";
  public static final String CGSOLVER = "CGSOLVER";
  public static final String GUI = "GUI";
  public static final String FLAMELET = "FLAMELET";
  public static final String FLAMELET_FUNC = "FLAMELET_FUNC";
  public static final String CAVITATION = "CAVITATION";
  public static final String VOF = "VOF";
  public static final String CHEMCNTL = "CHEMCNTL";
  public static final String USRSUB = "USRSUB";
  public static final String KEMODEL = "KEMODEL";
  public static final String KOMGMODEL = "KOMGMODEL";
  public static final String PARTICLE_MODEL = "PARTICLE_MODEL";
  public static final String INJECTOR_NUMBER = "INJECTOR_NUMBER";
  public static final String SOUND = "SOUND";
  public static final String EUL2PH = "EUL2PH";
  public static final String HPC_CNTL = "HPC_CNTL";
  public static final String MERGE_OR_REDECOMPOSITION = "MERGE_OR_REDECOMPOSITION";
  public static final String HPC_MERGE = "HPC_MERGE";
  public static final String HPC_REDECOMP = "HPC_REDECOMP";
  public static final String SIZES = "SIZES";
  public static final String RADOPTION = "RADOPTION";

  // ── Multi-instance namelists ───────────────────────────────────────────────

  public static final String BOUNDARY = "BOUNDARY";
  public static final String FLUID = "FLUID";
  public static final String SOLID = "SOLID";
  public static final String INITIAL = "INITIAL";
  public static final String OUTPUT = "OUTPUT";
  public static final String MASS = "MASS";
  public static final String ENERGY = "ENERGY";
  public static final String SOUND_SOURCE = "SOUND_SOURCE";
  public static final String SOUND_OBSERVER = "SOUND_OBSERVER";
  public static final String PROBES = "PROBES";
  public static final String FORCE_FLUIDS = "FORCE_FLUIDS";
  public static final String CDCL_OUTPUT = "CDCL_OUTPUT";
  public static final String SPECIES = "SPECIES";
  public static final String CHEMREAC = "CHEMREAC";
  public static final String VOF_INIT = "VOF_INIT";
  public static final String CAVI_INIT = "CAVI_INIT";
  public static final String SURFACE_SPECIES = "SURFACE_SPECIES";
}
