package affr.data;

import java.nio.file.Path;

/**
 * A directory that contains the AFFr project-marker file ({@code .affr_project}).
 *
 * <p>The {@link #memo()} is the free-text content of {@code .affr_project}. It is empty when the
 * file exists but has no content, and empty when the file is unreadable.
 *
 * <p>This is a lightweight browser-level snapshot of a project — it carries only what is needed to
 * render a card in the File-browser view. Loading the full project domain object (calculations,
 * inputs, etc.) is a separate, deferred concern handled by {@code affr.project}.
 */
public record ProjectEntry(Path path, String name, String memo) implements BrowserEntry {}
