package affr.project;

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Domain object for a single CFD calculation inside an {@link AFFrProject}.
 *
 * <p>A calculation is the fundamental unit of work: one complete solver case with its own mesh,
 * physics settings, execution history, and result files.
 *
 * <p>This class implements {@link ProjectItem} so it can be held in {@link AFFrProject}'s item list
 * alongside future item types (mesh generators, optimisers, etc.).
 *
 * <p>The full solver input ({@code AFFrInput}) is <em>not</em> loaded here; it is a deferred
 * concern handled when the Input Editor opens.
 *
 * <p>{@link #property} and {@link #model} are mutable fields because they change over the
 * calculation's lifetime (status transitions, model re-selection). Callers replace the whole record
 * rather than mutating individual fields, keeping each snapshot immutable.
 */
public final class AFFrCalculation implements ProjectItem {

  private final String name;
  private final Path path;

  // Back-reference to the owning project. Initially null during two-pass construction in
  // ProjectLoader; set immediately after AFFrProject is built.
  private @Nullable AFFrProject project;

  private AFFrCalProperty property;
  private AFFrCalculationModel model;

  /**
   * Creates a calculation with the given identity and initial sub-objects.
   *
   * @param name display name (e.g. {@code "cal_01"})
   * @param path absolute path to the calculation directory
   * @param project the project that owns this calculation; may be {@code null} during two-pass
   *     construction in {@link ProjectLoader} and must be set via {@link #setProject} before use
   * @param property persistent metadata snapshot
   * @param model physics model selection snapshot
   */
  public AFFrCalculation(
      String name,
      Path path,
      @Nullable AFFrProject project,
      AFFrCalProperty property,
      AFFrCalculationModel model) {
    this.name = name;
    this.path = path;
    this.project = project;
    this.property = property;
    this.model = model;
  }

  // ── ProjectItem ────────────────────────────────────────────────────────────

  /** Display name of this calculation (e.g. {@code "cal_01"}). */
  @Override
  public String name() {
    return name;
  }

  /** Absolute path to this calculation's directory on disk. */
  @Override
  public Path path() {
    return path;
  }

  /**
   * ISO-8601 date of the last modification, delegated to {@link AFFrCalProperty#date()}. Empty
   * string if not yet set.
   */
  @Override
  public String date() {
    return property.date();
  }

  // ── Sub-object accessors ───────────────────────────────────────────────────

  /**
   * The project that owns this calculation.
   *
   * @throws IllegalStateException if called before the project back-reference has been set
   */
  public AFFrProject getProject() {
    AFFrProject p = project;
    if (p == null) throw new IllegalStateException("project back-reference not yet set");
    return p;
  }

  /** Sets the owning project. Used by {@link ProjectLoader} during two-pass construction. */
  void setProject(AFFrProject project) {
    this.project = project;
  }

  /** The persistent metadata snapshot for this calculation. */
  public AFFrCalProperty getProperty() {
    return property;
  }

  /**
   * Replaces the persistent metadata. Called when a status transition occurs or metadata is re-read
   * from disk.
   */
  public void setProperty(AFFrCalProperty property) {
    this.property = property;
  }

  /** The physics model selection for this calculation. */
  public AFFrCalculationModel getModel() {
    return model;
  }

  /** Replaces the physics model selection. */
  public void setModel(AFFrCalculationModel model) {
    this.model = model;
  }

  // ── Convenience accessors ──────────────────────────────────────────────────

  /** Current lifecycle status, delegated to {@link AFFrCalProperty#status()}. */
  public CalculationStatus getStatus() {
    return property.status();
  }
}
