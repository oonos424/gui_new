package affr.app.top;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link PathFormatting}. */
final class PathFormattingTest {

  @Test
  void breadcrumbAtRootShowsBareTilde() {
    Path root = Path.of("ws-root");
    assertEquals("~/.affr", PathFormatting.breadcrumb(root, root));
  }

  @Test
  void breadcrumbForSingleSegmentChild() {
    Path root = Path.of("ws-root");
    Path child = root.resolve("projects");
    assertEquals("~/.affr/projects", PathFormatting.breadcrumb(child, root));
  }

  @Test
  void breadcrumbUsesForwardSlashesRegardlessOfPlatform(@TempDir Path tempRoot) {
    // Build a real, multi-segment path inside @TempDir so we exercise the host
    // filesystem's native separator. On Windows, root.relativize(child).toString()
    // would otherwise contain backslashes — the breadcrumb formatter must normalise
    // these to '/' so users see the same output on Windows and POSIX.
    Path child = tempRoot.resolve("alpha").resolve("beta").resolve("gamma");

    String formatted = PathFormatting.breadcrumb(child, tempRoot);

    assertEquals("~/.affr/alpha/beta/gamma", formatted);
  }

  // ── breadcrumb with custom root label ────────────────────────────────────

  @Test
  void breadcrumbWithCustomRootLabelAtRootShowsLabel() {
    Path root = Path.of("ws-root");
    assertEquals("tutorials", PathFormatting.breadcrumb(root, root, "tutorials"));
  }

  @Test
  void breadcrumbWithCustomRootLabelForChildAppendssegment() {
    Path root = Path.of("ws-root");
    Path child = root.resolve("CASE1_Bump");
    assertEquals("tutorials/CASE1_Bump", PathFormatting.breadcrumb(child, root, "tutorials"));
  }

  @Test
  void breadcrumbWithCustomRootLabelForNestedChild() {
    Path root = Path.of("ws-root");
    Path child = root.resolve("cases").resolve("bump");
    assertEquals("tutorials/cases/bump", PathFormatting.breadcrumb(child, root, "tutorials"));
  }
}
