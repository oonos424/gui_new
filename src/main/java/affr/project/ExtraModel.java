package affr.project;

/**
 * Optional physics model extensions for a calculation.
 *
 * <p>Zero or more of these may be active simultaneously. Enabling an extra model activates the
 * corresponding Input Editor tab and adds the relevant namelists to {@code AFFrInput}.
 *
 * <p>Persisted in {@code .mode} as a JSON array of enum names (e.g. {@code ["VOF",
 * "PARTICLE_TRACK"]}).
 */
public enum ExtraModel {
  VOF,
  GHOST_FLUID,
  CAVITATION,
  COMBUST_CHEM_REACT,
  COMBUSTION,
  RADIATION,
  SURFACE_REACTION,
  PARTICLE_TRACK,
  POROUS_MODEL,
  ROTATING_FRAME,
  MOVING_MESH,
  OVERSET_GRID,
}
