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
   * Formats a workspace-relative path as {@code ~/.affr} or {@code ~/.affr/relative/sub/path}.
   *
   * <p>Both arguments must come from the same default filesystem. The output always uses {@code
   * '/'} as a separator, even on Windows where {@link Path#toString()} would otherwise return
   * backslashes.
   */
  public static String breadcrumb(Path current, Path root) {
    if (current.equals(root)) {
      return "~/.affr";
    }
    Path relative = root.relativize(current);
    StringBuilder sb = new StringBuilder("~/.affr");
    for (Path segment : relative) {
      sb.append('/').append(segment);
    }
    return sb.toString();
  }
}
