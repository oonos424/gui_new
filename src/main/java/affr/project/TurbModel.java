package affr.project;

/**
 * Turbulence model for a calculation.
 *
 * <p>Persisted in {@code .mode} as the enum name (e.g. {@code "RANS"}).
 */
public enum TurbModel {
  LES,
  RANS,
  DNS,
  NO,
}
