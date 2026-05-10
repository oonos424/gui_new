package affr.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link PathFx}. */
final class PathFxTest {

  @Test
  void toExistingDirReturnsNullForNull() {
    assertNull(PathFx.toExistingDir(null));
  }

  @Test
  void toExistingDirReturnsNullForNonExistentPath(@TempDir Path tmp) {
    assertNull(PathFx.toExistingDir(tmp.resolve("does-not-exist")));
  }

  @Test
  void toExistingDirReturnsNullForRegularFile(@TempDir Path tmp) throws Exception {
    Path file = tmp.resolve("a-file.txt");
    Files.writeString(file, "x");
    assertNull(PathFx.toExistingDir(file));
  }

  @Test
  void toExistingDirReturnsFileForExistingDirectory(@TempDir Path tmp) throws Exception {
    Path subdir = Files.createDirectory(tmp.resolve("subdir"));
    File f = PathFx.toExistingDir(subdir);
    assertNotNull(f);
    assertEquals(subdir.toFile(), f);
  }

  @Test
  void toFileRoundTripsThroughPath(@TempDir Path tmp) {
    Path p = tmp.resolve("anything");
    assertEquals(p.toFile(), PathFx.toFile(p));
  }

  @Test
  void fromChooserRoundTripsThroughFile(@TempDir Path tmp) {
    File f = tmp.resolve("anything").toFile();
    assertEquals(f.toPath(), PathFx.fromChooser(f));
  }
}
