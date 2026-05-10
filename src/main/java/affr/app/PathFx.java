package affr.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Single conversion point between {@link Path} (the canonical type for paths everywhere in this
 * codebase) and {@link File}, which is required only by a small number of JavaFX and AWT APIs (e.g.
 * {@link javafx.stage.FileChooser#setInitialDirectory(File)}, {@link java.awt.Desktop#open(File)}).
 *
 * <p>Production code outside the View layer must not import {@link File}; if a {@link File}
 * conversion is needed it must go through one of the helpers here so the conversion lives in
 * exactly one place.
 */
public final class PathFx {

  private PathFx() {}

  /**
   * Returns {@code dir.toFile()} if {@code dir} is non-null and points to an existing directory;
   * returns {@code null} otherwise. Useful for {@link
   * javafx.stage.FileChooser#setInitialDirectory(File)} which silently ignores {@code null} but
   * throws if given a non-existent directory.
   */
  public static @Nullable File toExistingDir(@Nullable Path dir) {
    if (dir == null) return null;
    if (!Files.isDirectory(dir)) return null;
    return dir.toFile();
  }

  /** Bare {@link Path#toFile()} wrapper to keep the conversion in one named place. */
  public static File toFile(Path path) {
    return path.toFile();
  }

  /** Bare {@link File#toPath()} wrapper to keep the conversion in one named place. */
  public static Path fromChooser(File file) {
    return file.toPath();
  }
}
