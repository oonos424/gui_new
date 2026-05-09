package affr.data;

import java.nio.file.Path;

/**
 * A single entry shown in the File-browser view.
 *
 * <p>Sealed so that the compiler can verify exhaustive pattern-matching in switch expressions. Only
 * two kinds exist: a plain navigable {@link FolderEntry} and an AFFr {@link ProjectEntry}.
 *
 * <p>Both subtypes are records, ensuring value-based equality and immutability.
 */
public sealed interface BrowserEntry permits FolderEntry, ProjectEntry {

  /** The absolute path to this entry on the local filesystem. */
  Path path();

  /** The display name (the last path component). */
  String name();
}
