package affr.project;

import java.util.Map;

/**
 * Persistent metadata for a single calculation, stored in {@code .affr_property} (JSON).
 *
 * <p>This record is an immutable snapshot. When a status transition occurs, the caller replaces the
 * {@code property} field on the owning {@link AFFrCalculation} with a new instance.
 *
 * <p>{@link #DEFAULT} represents the state of a freshly created calculation before any run.
 */
public record AFFrCalProperty(
    CalculationStatus status,
    String date,
    int timeStep,
    String host,
    String jobId,
    String queueName,
    int ncpu,
    boolean userSubrtUsed,
    Map<String, String> execFiles,
    Map<String, Boolean> usrsubCheck) {

  /** Default property for a freshly created calculation. */
  public static final AFFrCalProperty DEFAULT =
      new AFFrCalProperty(
          CalculationStatus.SETTING, "", 0, "localhost", "", "未設定", 1, false, Map.of(), Map.of());
}
