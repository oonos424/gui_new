package affr.util.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FsLoader}.
 *
 * <p>All tests use a JUnit {@link TempDir} for full filesystem isolation; they never touch the
 * real user home directory.
 */
final class FsLoaderTest {

  // -------------------------------------------------------------------------
  // Empty / non-existent
  // -------------------------------------------------------------------------

  @Test
  void emptyDirectoryReturnsEmptyList(@TempDir Path dir) throws IOException {
    List<Path> result = FsLoader.listChildDirs(dir);

    assertTrue(result.isEmpty());
  }

  @Test
  void nonExistentDirectoryThrowsIOException(@TempDir Path dir) {
    Path missing = dir.resolve("does_not_exist");

    assertThrows(IOException.class, () -> FsLoader.listChildDirs(missing));
  }

  // -------------------------------------------------------------------------
  // Only directories are returned
  // -------------------------------------------------------------------------

  @Test
  void regularFilesAreExcluded(@TempDir Path dir) throws IOException {
    Files.createFile(dir.resolve("file.txt"));
    Files.createFile(dir.resolve("fflow.ctl"));
    Files.createDirectory(dir.resolve("subdir"));

    List<Path> result = FsLoader.listChildDirs(dir);

    assertEquals(1, result.size());
    assertEquals("subdir", result.get(0).getFileName().toString());
  }

  // -------------------------------------------------------------------------
  // Hidden entries excluded
  // -------------------------------------------------------------------------

  @Test
  void hiddenDirsAreExcluded(@TempDir Path dir) throws IOException {
    Files.createDirectory(dir.resolve(".hidden"));
    Files.createDirectory(dir.resolve("visible"));

    List<Path> result = FsLoader.listChildDirs(dir);

    assertEquals(1, result.size());
    assertEquals("visible", result.get(0).getFileName().toString());
  }

  @Test
  void hiddenFilesAreExcluded(@TempDir Path dir) throws IOException {
    Files.createFile(dir.resolve(".affr_project"));
    Files.createDirectory(dir.resolve("proj"));

    List<Path> result = FsLoader.listChildDirs(dir);

    assertEquals(1, result.size());
  }

  // -------------------------------------------------------------------------
  // Sort order: case-insensitive alphabetical
  // -------------------------------------------------------------------------

  @Test
  void dirsAreSortedCaseInsensitively(@TempDir Path dir) throws IOException {
    Files.createDirectory(dir.resolve("Zebra"));
    Files.createDirectory(dir.resolve("apple"));
    Files.createDirectory(dir.resolve("Mango"));

    List<Path> result = FsLoader.listChildDirs(dir);

    assertEquals(
        List.of("apple", "Mango", "Zebra"),
        result.stream().map(p -> p.getFileName().toString()).toList());
  }

  @Test
  void multipleDirectoriesReturnedInOrder(@TempDir Path dir) throws IOException {
    for (String name : new String[]{"gamma", "alpha", "beta"}) {
      Files.createDirectory(dir.resolve(name));
    }

    List<Path> result = FsLoader.listChildDirs(dir);

    assertEquals(
        List.of("alpha", "beta", "gamma"),
        result.stream().map(p -> p.getFileName().toString()).toList());
  }
}
