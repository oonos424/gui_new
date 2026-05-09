package affr.project;

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
 * Unit tests for {@link ProjectWriter}.
 *
 * <p>Tests cover name generation, calculation creation, and JSON file writing. Round-trip tests
 * verify that files written by {@link ProjectWriter} are read back correctly by {@link
 * ProjectLoader}.
 */
final class ProjectWriterTest {

  // ── nextCalName ───────────────────────────────────────────────────────────

  @Test
  void firstCalNameIsCal01WithEmptyList() {
    assertEquals("cal_01", ProjectWriter.nextCalName(List.of()));
  }

  @Test
  void nextCalNameIncrementsByOne() {
    List<ProjectItem> items =
        List.of(
            new AFFrCalculation(
                "cal_01",
                Path.of("/p/cal_01"),
                null,
                AFFrCalProperty.DEFAULT,
                AFFrCalculationModel.DEFAULT));
    assertEquals("cal_02", ProjectWriter.nextCalName(items));
  }

  @Test
  void nextCalNameSkipsToMaxPlusOne() {
    List<ProjectItem> items =
        List.of(
            new AFFrCalculation(
                "cal_01",
                Path.of("/p/cal_01"),
                null,
                AFFrCalProperty.DEFAULT,
                AFFrCalculationModel.DEFAULT),
            new AFFrCalculation(
                "cal_03",
                Path.of("/p/cal_03"),
                null,
                AFFrCalProperty.DEFAULT,
                AFFrCalculationModel.DEFAULT));
    assertEquals("cal_04", ProjectWriter.nextCalName(items));
  }

  @Test
  void nextCalNameIgnoresNonCalNames() {
    List<ProjectItem> items =
        List.of(
            new AFFrCalculation(
                "mesh_01",
                Path.of("/p/mesh_01"),
                null,
                AFFrCalProperty.DEFAULT,
                AFFrCalculationModel.DEFAULT));
    assertEquals("cal_01", ProjectWriter.nextCalName(items));
  }

  @Test
  void nextCalNamePadsToTwoDigits() {
    List<ProjectItem> items =
        List.of(
            new AFFrCalculation(
                "cal_09",
                Path.of("/p/cal_09"),
                null,
                AFFrCalProperty.DEFAULT,
                AFFrCalculationModel.DEFAULT));
    assertEquals("cal_10", ProjectWriter.nextCalName(items));
  }

  // ── createCalculation ─────────────────────────────────────────────────────

  @Test
  void createCalculationMakesDirectory(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, List.of(), project);

    assertTrue(Files.isDirectory(proj.resolve("cal_01")));
  }

  @Test
  void createCalculationWritesCalPropertyFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, List.of(), project);

    assertTrue(Files.exists(proj.resolve("cal_01").resolve(".affr_property")));
  }

  @Test
  void createCalculationWritesModeFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, List.of(), project);

    assertTrue(Files.exists(proj.resolve("cal_01").resolve(".mode")));
  }

  @Test
  void createCalculationReturnsCorrectName(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    assertEquals("cal_01", cal.name());
  }

  @Test
  void createCalculationSetsProjectBackReference(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    assertEquals(project, cal.getProject());
  }

  @Test
  void createCalculationSequencesNames(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation first = ProjectWriter.createCalculation(proj, List.of(), project);
    AFFrCalculation second = ProjectWriter.createCalculation(proj, List.of(first), project);

    assertEquals("cal_01", first.name());
    assertEquals("cal_02", second.name());
  }

  @Test
  void createCalculationThrowsIfDirectoryAlreadyExists(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createDirectory(proj.resolve("cal_01")); // pre-existing
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    assertThrows(
        IOException.class, () -> ProjectWriter.createCalculation(proj, List.of(), project));
  }

  // ── Round-trip: write → load ──────────────────────────────────────────────

  @Test
  void calPropertyRoundTrip(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(".affr_project"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, List.of(), project);
    AFFrProject loaded = new ProjectLoader().load(proj);
    AFFrCalculation cal = (AFFrCalculation) loaded.getItems().get(0);

    assertEquals(CalculationStatus.SETTING, cal.getStatus());
    assertEquals("", cal.date());
    assertEquals(1, cal.getProperty().ncpu());
    assertEquals("localhost", cal.getProperty().host());
  }

  @Test
  void calModelRoundTrip(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(".affr_project"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, List.of(), project);
    AFFrProject loaded = new ProjectLoader().load(proj);
    AFFrCalculation cal = (AFFrCalculation) loaded.getItems().get(0);

    assertEquals(AFFrCalculationModel.DEFAULT, cal.getModel());
  }
}
