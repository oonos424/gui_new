package affr.app.top;

import java.nio.file.Path;

/**
 * Path-display formatting for the View layer.
 *
 * <p>This is the single place in the codebase where {@link Path} instances are converted to
 * human-facing display strings. Display strings always use {@code '/'} as the separator regardless
 * of the host platform, so users see the same breadcrumb on Windows and POSIX.
 */
public final class PathFormatting {

  private PathFormatting() {}

  /**
   * Formats a workspace-relative path as {@code rootLabel} or {@code rootLabel/relative/sub/path}.
   *
   * <p>Both path arguments must come from the same default filesystem. The output always uses
   * {@code '/'} as a separator, even on Windows where {@link Path#toString()} would otherwise
   * return backslashes.
   *
   * @param current the path currently displayed
   * @param root the root of the browseable tree (browser cannot navigate above this)
   * @param rootLabel the human-readable label for {@code root} (e.g. {@code "~/.affr"} for the user
   *     workspace, {@code "tutorials"} for the tutorial inventory root)
   */
  public static String breadcrumb(Path current, Path root, String rootLabel) {
    if (current.equals(root)) {
      return rootLabel;
    }
    Path relative = root.relativize(current);
    StringBuilder sb = new StringBuilder(rootLabel);
    for (Path segment : relative) {
      sb.append('/').append(segment);
    }
    return sb.toString();
  }

  /**
   * Convenience overload that uses {@code "~/.affr"} as the root label. Suitable for the user
   * workspace browser where the root is always the AFFr application directory.
   */
  public static String breadcrumb(Path current, Path root) {
    return breadcrumb(current, root, "~/.affr");
  }
}
