package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
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

  // ── createCalculation(model) ──────────────────────────────────────────────

  @Test
  void createCalculationWithModelReturnsCorrectModel(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculationModel model =
        new AFFrCalculationModel(
            ComprsModel.COMPRESSIBLE,
            SteadyModel.UNSTEADY,
            TurbModel.LES,
            Set.of(ExtraModel.MOVING_MESH));

    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project, model);

    assertEquals(model, cal.getModel());
  }

  @Test
  void createCalculationWithModelPersistsToModeFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(".affr_project"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculationModel model =
        new AFFrCalculationModel(
            ComprsModel.COMPRESSIBLE,
            SteadyModel.UNSTEADY,
            TurbModel.LES,
            Set.of(ExtraModel.MOVING_MESH));

    ProjectWriter.createCalculation(proj, List.of(), project, model);
    AFFrProject loaded = new ProjectLoader().load(proj);
    AFFrCalculation cal = (AFFrCalculation) loaded.getItems().get(0);

    assertEquals(model, cal.getModel());
  }

  @Test
  void createCalculationDefaultOverloadUsesDefaultModel(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    assertEquals(AFFrCalculationModel.DEFAULT, cal.getModel());
  }

  // ── createCalculation(named) ──────────────────────────────────────────────

  @Test
  void createNamedCalculationMakesDirectoryWithSuppliedName(@TempDir Path root)
      throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, "my_run", List.of(), project, AFFrCalculationModel.DEFAULT);

    assertTrue(Files.isDirectory(proj.resolve("my_run")));
  }

  @Test
  void createNamedCalculationReturnsCalculationWithCorrectName(@TempDir Path root)
      throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation cal =
        ProjectWriter.createCalculation(proj, "my_run", List.of(), project, AFFrCalculationModel.DEFAULT);

    assertEquals("my_run", cal.name());
  }

  @Test
  void createNamedCalculationWritesCalPropertyAndModeFiles(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    ProjectWriter.createCalculation(proj, "my_run", List.of(), project, AFFrCalculationModel.DEFAULT);

    assertTrue(Files.exists(proj.resolve("my_run").resolve(".affr_property")));
    assertTrue(Files.exists(proj.resolve("my_run").resolve(".mode")));
  }

  @Test
  void createNamedCalculationSetsProjectBackReference(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation cal =
        ProjectWriter.createCalculation(proj, "my_run", List.of(), project, AFFrCalculationModel.DEFAULT);

    assertEquals(project, cal.getProject());
  }

  @Test
  void createNamedCalculationStoresSuppliedModel(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculationModel model =
        new AFFrCalculationModel(
            ComprsModel.COMPRESSIBLE, SteadyModel.UNSTEADY, TurbModel.LES, Set.of());

    AFFrCalculation cal = ProjectWriter.createCalculation(proj, "my_run", List.of(), project, model);

    assertEquals(model, cal.getModel());
  }

  @Test
  void createNamedCalculationThrowsForBlankName(@TempDir Path root) {
    Path proj = root.resolve("proj");
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    assertThrows(
        IOException.class,
        () ->
            ProjectWriter.createCalculation(
                proj, "   ", List.of(), project, AFFrCalculationModel.DEFAULT));
  }

  @Test
  void createNamedCalculationThrowsForEmptyName(@TempDir Path root) {
    Path proj = root.resolve("proj");
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    assertThrows(
        IOException.class,
        () ->
            ProjectWriter.createCalculation(
                proj, "", List.of(), project, AFFrCalculationModel.DEFAULT));
  }

  @Test
  void createNamedCalculationThrowsOnCaseInsensitiveCollision(@TempDir Path root)
      throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation existing = ProjectWriter.createCalculation(proj, List.of(), project);

    assertThrows(
        IOException.class,
        () ->
            ProjectWriter.createCalculation(
                proj, "CAL_01", List.of(existing), project, AFFrCalculationModel.DEFAULT));
  }

  @Test
  void createNamedCalculationThrowsIfDirectoryAlreadyExists(@TempDir Path root)
      throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createDirectory(proj.resolve("existing_dir")); // pre-existing dir not in items list
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    assertThrows(
        IOException.class,
        () ->
            ProjectWriter.createCalculation(
                proj, "existing_dir", List.of(), project, AFFrCalculationModel.DEFAULT));
  }

  @Test
  void createNamedCalculationTrimsLeadingAndTrailingWhitespace(@TempDir Path root)
      throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());

    AFFrCalculation cal =
        ProjectWriter.createCalculation(
            proj, "  my_run  ", List.of(), project, AFFrCalculationModel.DEFAULT);

    assertEquals("my_run", cal.name());
    assertTrue(Files.isDirectory(proj.resolve("my_run")));
  }

  // ── renameCalculation ────────────────────────────────────────────────────

  @Test
  void renameRenamesDirectoryOnDisk(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    ProjectWriter.renameCalculation(proj, cal, "cal_renamed", List.of(cal));

    assertTrue(Files.isDirectory(proj.resolve("cal_renamed")));
    assertFalse(Files.exists(proj.resolve("cal_01")));
  }

  @Test
  void renameReturnsCalculationWithNewName(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation renamed =
        ProjectWriter.renameCalculation(proj, cal, "cal_renamed", List.of(cal));

    assertEquals("cal_renamed", renamed.name());
  }

  @Test
  void renameReturnsCalculationWithNewPath(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation renamed =
        ProjectWriter.renameCalculation(proj, cal, "cal_renamed", List.of(cal));

    assertEquals(proj.resolve("cal_renamed"), renamed.path());
  }

  @Test
  void renamePreservesProjectBackReference(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation renamed =
        ProjectWriter.renameCalculation(proj, cal, "cal_renamed", List.of(cal));

    assertEquals(project, renamed.getProject());
  }

  @Test
  void renamePreservesPropertyAndModel(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation renamed =
        ProjectWriter.renameCalculation(proj, cal, "cal_renamed", List.of(cal));

    assertEquals(cal.getProperty(), renamed.getProperty());
    assertEquals(cal.getModel(), renamed.getModel());
  }

  @Test
  void renameThrowsForBlankName(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    assertThrows(
        IOException.class, () -> ProjectWriter.renameCalculation(proj, cal, "   ", List.of(cal)));
  }

  @Test
  void renameThrowsOnCaseInsensitiveCollision(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal1 = ProjectWriter.createCalculation(proj, List.of(), project);
    AFFrCalculation cal2 = ProjectWriter.createCalculation(proj, List.of(cal1), project);

    assertThrows(
        IOException.class,
        () -> ProjectWriter.renameCalculation(proj, cal1, "CAL_02", List.of(cal1, cal2)));
  }

  // ── copyCalculation ───────────────────────────────────────────────────────

  @Test
  void copyCreatesNewDirectory(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertTrue(Files.isDirectory(proj.resolve("cal_02")));
  }

  @Test
  void copyUsesNextSequentialName(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation copy = ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertEquals("cal_02", copy.name());
  }

  @Test
  void copyPreservesProjectBackReference(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation copy = ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertEquals(project, copy.getProject());
  }

  @Test
  void copyResetsStatusToSetting(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation copy = ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertEquals(CalculationStatus.SETTING, copy.getStatus());
  }

  @Test
  void copyPreservesModelSettings(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    AFFrCalculation copy = ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertEquals(cal.getModel(), copy.getModel());
  }

  @Test
  void copyCopiesAllFilesIntoDestination(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);
    Files.writeString(cal.path().resolve("extra.dat"), "data");

    ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertTrue(Files.exists(proj.resolve("cal_02").resolve("extra.dat")));
  }

  @Test
  void copyCopiesNestedSubdirectories(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);
    Path subDir = Files.createDirectory(cal.path().resolve("results"));
    Files.writeString(subDir.resolve("out.dat"), "result");

    ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertTrue(Files.exists(proj.resolve("cal_02").resolve("results").resolve("out.dat")));
  }

  @Test
  void copyDoesNotModifySourceDirectory(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);

    assertTrue(Files.isDirectory(cal.path()), "source directory must still exist after copy");
  }

  // ── deleteCalculation ─────────────────────────────────────────────────────

  @Test
  void deleteRemovesDirectory(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    ProjectWriter.deleteCalculation(cal);

    assertFalse(Files.exists(cal.path()));
  }

  @Test
  void deleteRemovesAllFilesRecursively(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);
    Files.writeString(cal.path().resolve("log.dat"), "log");
    Path subDir = Files.createDirectory(cal.path().resolve("results"));
    Files.writeString(subDir.resolve("out.dat"), "result");

    ProjectWriter.deleteCalculation(cal);

    assertFalse(Files.exists(cal.path()));
  }

  // ── Round-trip: rename / copy → load ─────────────────────────────────────

  @Test
  void renameRoundTrip(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(".affr_project"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    ProjectWriter.renameCalculation(proj, cal, "cal_renamed", List.of(cal));
    AFFrProject loaded = new ProjectLoader().load(proj);

    assertEquals(1, loaded.getItems().size());
    assertEquals("cal_renamed", loaded.getItems().get(0).name());
  }

  @Test
  void copyRoundTrip(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(".affr_project"));
    AFFrProject project = new AFFrProject("proj", proj, "", List.of());
    AFFrCalculation cal = ProjectWriter.createCalculation(proj, List.of(), project);

    ProjectWriter.copyCalculation(proj, cal, List.of(cal), project);
    AFFrProject loaded = new ProjectLoader().load(proj);

    assertEquals(2, loaded.getItems().size());
  }

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
