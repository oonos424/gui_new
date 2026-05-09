package affr.project;

import java.util.Set;

/**
 * Physics model selection for a calculation, stored in {@code .mode} (JSON).
 *
 * <p>This record is an immutable snapshot. The four selections are orthogonal: compressibility,
 * steady/unsteady, turbulence approach, and zero or more optional model extensions.
 *
 * <p>{@link #DEFAULT} represents the standard starting configuration (Incompressible, Steady, RANS,
 * no extras).
 */
public record AFFrCalculationModel(
    ComprsModel comprsModel,
    SteadyModel steadyModel,
    TurbModel turbModel,
    Set<ExtraModel> extraModelSet) {

  /** Default model: Incompressible, Steady, RANS, no extra models. */
  public static final AFFrCalculationModel DEFAULT =
      new AFFrCalculationModel(
          ComprsModel.INCOMPRESSIBLE, SteadyModel.STEADY, TurbModel.RANS, Set.of());
}
