package affr.util.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Raw filesystem IO: lists the direct child directories of a given path.
 *
 * <p>This class has no knowledge of AFFr domain concepts (projects, calculations, etc.) — it knows
 * only about the filesystem. Domain interpretation lives in {@code affr.data.DataStore}.
 *
 * <p>All public methods perform IO and must be called from a background thread, never from the
 * JavaFX Application Thread.
 */
public final class FsLoader {

  private FsLoader() {}

  /**
   * Returns the direct child directories of {@code dir}, sorted case-insensitively by name. Hidden
   * entries (names starting with {@code .}) are excluded.
   *
   * @param dir the directory to list; must exist and be readable
   * @throws IOException if the directory cannot be listed
   */
  public static List<Path> listChildDirs(Path dir) throws IOException {
    try (Stream<Path> stream = Files.list(dir)) {
      return stream
          .filter(
              p -> {
                Path fn = p.getFileName();
                String name = fn != null ? fn.toString() : "";
                return !name.startsWith(".") && Files.isDirectory(p);
              })
          .sorted(
              (a, b) -> {
                String an = a.getFileName() != null ? a.getFileName().toString() : "";
                String bn = b.getFileName() != null ? b.getFileName().toString() : "";
                return an.compareToIgnoreCase(bn);
              })
          .toList();
    }
  }
}
