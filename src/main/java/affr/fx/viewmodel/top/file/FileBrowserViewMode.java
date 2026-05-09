package affr.fx.viewmodel.top.file;

/**
 * Display mode for the file-browser item list.
 *
 * <p>New modes (e.g. {@code TREE}) are added here and handled exhaustively in {@link
 * affr.app.top.file.FileBrowserController} via a {@code switch} statement — the compiler enforces
 * coverage for every new constant.
 */
public enum FileBrowserViewMode {
  /** Compact single-column list — the default. */
  LIST,

  /** Grid of labelled icon tiles, one per entry. */
  ICON,

  /**
   * Hierarchical tree that lazily loads sub-directories on expansion. Projects are leaves; folders
   * expand to reveal their contents on demand.
   */
  TREE,
}
