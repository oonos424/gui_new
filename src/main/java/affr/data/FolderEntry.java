package affr.data;

import java.nio.file.Path;

/**
 * A plain directory that the user can navigate into but that is not an AFFr project.
 *
 * <p>Plain folders have no AFFr-specific metadata; they exist purely to group projects
 * hierarchically inside the workspace root.
 */
public record FolderEntry(Path path, String name) implements BrowserEntry {}
