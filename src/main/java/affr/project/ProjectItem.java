package affr.project;

import java.nio.file.Path;

/**
 * A single unit of work inside an {@link AFFrProject}.
 *
 * <p>Sealed so that every site that dispatches on item type can be verified exhaustive by the
 * compiler. Adding a new work type (e.g. {@code MeshItem}, {@code ParameterSurveyItem}) requires:
 *
 * <ol>
 *   <li>A new {@code permits} entry here.
 *   <li>A new marker-file check in {@link ProjectLoader}.
 *   <li>A new {@code case} in every {@code switch(item)} — the compiler enforces this.
 * </ol>
 */
public sealed interface ProjectItem permits AFFrCalculation {

  /** Display name (e.g. {@code "cal_01"}). */
  String name();

  /** Absolute path to this item's directory on the local filesystem. */
  Path path();

  /**
   * ISO-8601 date string of the last modification (e.g. {@code "2026-05-01"}). Empty string when
   * the date is unknown or not yet set.
   */
  String date();
}
