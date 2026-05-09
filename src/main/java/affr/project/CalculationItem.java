package affr.project;

import java.nio.file.Path;

/**
 * A lightweight snapshot of a single CFD calculation, sufficient for display in the project item
 * list.
 *
 * <p>This record carries only what the list view needs: name, path, status, and last-modification
 * date. Loading the full solver input ({@code AFFrInput}) is a separate, deferred concern handled
 * when the Input Editor opens.
 *
 * <p>Persisted on disk as a subdirectory containing {@code .affr_property} (JSON).
 */
public record CalculationItem(String name, Path path, CalculationStatus status, String date)
    implements ProjectItem {}
