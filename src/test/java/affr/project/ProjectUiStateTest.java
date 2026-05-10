package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ProjectUiState}.
 *
 * <p>Each test uses a {@link TempDir} acting as a fake project directory so that reads and writes
 * never touch real project files.
 */
final class ProjectUiStateTest {

  // ── Defaults (no file present) ─────────────────────────────────────────────

  @Test
  void defaultsWhenNoFileExists(@TempDir Path projDir) {
    ProjectUiState state = ProjectUiState.load(projDir);

    assertNull(state.getCtlLastDir(), "ctlLastDir should be null when no state file exists");
  }

  // ── ctlLastDir round-trip ──────────────────────────────────────────────────

  @Test
  void ctlLastDirRoundTrip(@TempDir Path projDir) {
    Path savedDir = projDir.resolve("calculations/run01");
    ProjectUiState state = ProjectUiState.load(projDir);
    state.setCtlLastDir(savedDir);
    state.save();

    ProjectUiState loaded = ProjectUiState.load(projDir);
    assertEquals(savedDir.toAbsolutePath(), loaded.getCtlLastDir());
  }

  @Test
  void ctlLastDirIsStoredAsAbsolutePath(@TempDir Path projDir) {
    Path dir = projDir.resolve("subdir");
    ProjectUiState state = ProjectUiState.load(projDir);
    state.setCtlLastDir(dir);
    state.save();

    ProjectUiState loaded = ProjectUiState.load(projDir);
    assertTrue(
        loaded.getCtlLastDir().isAbsolute(), "ctlLastDir should be stored as an absolute path");
  }

  @Test
  void unsavedCtlLastDirIsNull(@TempDir Path projDir) {
    ProjectUiState state = ProjectUiState.load(projDir);
    // Do NOT call setCtlLastDir / save
    assertNull(state.getCtlLastDir());
  }

  // ── File creation ──────────────────────────────────────────────────────────

  @Test
  void saveCreatesAffrUiFileInProjectDir(@TempDir Path projDir) {
    ProjectUiState state = ProjectUiState.load(projDir);
    state.setCtlLastDir(projDir.resolve("last"));
    state.save();

    assertTrue(
        Files.exists(projDir.resolve(ProjectUiState.FILENAME)),
        ".affr_ui should exist after save()");
  }

  @Test
  void saveCreatesFileEvenWithNothingSet(@TempDir Path projDir) {
    ProjectUiState state = ProjectUiState.load(projDir);
    state.save();

    assertTrue(
        Files.exists(projDir.resolve(ProjectUiState.FILENAME)),
        ".affr_ui should be created even when no values are set");
  }

  // ── Edge cases ─────────────────────────────────────────────────────────────

  @Test
  void emptyFileYieldsNull(@TempDir Path projDir) throws IOException {
    Files.createFile(projDir.resolve(ProjectUiState.FILENAME));

    ProjectUiState state = ProjectUiState.load(projDir);
    assertNull(state.getCtlLastDir());
  }

  @Test
  void corruptPathValueYieldsNull(@TempDir Path projDir) throws IOException {
    // Write a syntactically valid properties file but with a value that is
    // not a valid path on any platform (null character).  Path.of() will
    // throw an InvalidPathException, which getCtlLastDir() must swallow.
    Files.writeString(projDir.resolve(ProjectUiState.FILENAME), "ctl.lastDir=\u0000invalid\n");

    ProjectUiState state = ProjectUiState.load(projDir);
    assertNull(state.getCtlLastDir(), "corrupt path value should degrade to null");
  }

  @Test
  void saveIsIdempotent(@TempDir Path projDir) {
    Path dir = projDir.resolve("data");
    ProjectUiState state = ProjectUiState.load(projDir);
    state.setCtlLastDir(dir);
    state.save();
    state.save(); // second save must not corrupt the file

    ProjectUiState loaded = ProjectUiState.load(projDir);
    assertEquals(dir.toAbsolutePath(), loaded.getCtlLastDir());
  }

  @Test
  void laterSaveOverwritesEarlierValue(@TempDir Path projDir) {
    Path first = projDir.resolve("first");
    Path second = projDir.resolve("second");

    ProjectUiState state = ProjectUiState.load(projDir);
    state.setCtlLastDir(first);
    state.save();

    ProjectUiState state2 = ProjectUiState.load(projDir);
    state2.setCtlLastDir(second);
    state2.save();

    ProjectUiState loaded = ProjectUiState.load(projDir);
    assertEquals(second.toAbsolutePath(), loaded.getCtlLastDir());
  }

  // ── Isolation between projects ─────────────────────────────────────────────

  @Test
  void twoProjectsHaveIndependentState(@TempDir Path root) throws IOException {
    Path projA = Files.createDirectory(root.resolve("projA"));
    Path projB = Files.createDirectory(root.resolve("projB"));

    Path dirA = projA.resolve("calA");
    Path dirB = projB.resolve("calB");

    ProjectUiState stateA = ProjectUiState.load(projA);
    stateA.setCtlLastDir(dirA);
    stateA.save();

    ProjectUiState stateB = ProjectUiState.load(projB);
    stateB.setCtlLastDir(dirB);
    stateB.save();

    assertEquals(dirA.toAbsolutePath(), ProjectUiState.load(projA).getCtlLastDir());
    assertEquals(dirB.toAbsolutePath(), ProjectUiState.load(projB).getCtlLastDir());
  }
}
