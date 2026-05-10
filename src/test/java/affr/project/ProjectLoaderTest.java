package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ProjectLoader}.
 *
 * <p>All tests use a JUnit {@link TempDir} for full isolation; they never touch the real {@code
 * ~/.affr/} workspace. The JSON written by each test mirrors the format that the application itself
 * writes, exercising the full round-trip.
 */
final class ProjectLoaderTest {

  private static final String PROJECT_MARKER = ".affr_project";
  private static final String CAL_PROPERTY = ".affr_property";
  private static final String CAL_MODE = ".mode";

  // ── Project-level fields ──────────────────────────────────────────────────

  @Test
  void projectNameIsDirectoryName(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("my_project"));
    Files.createFile(proj.resolve(PROJECT_MARKER));

    AFFrProject project = new ProjectLoader().load(proj);

    assertEquals("my_project", project.getName());
  }

  @Test
  void projectPathMatchesArgument(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(PROJECT_MARKER));

    AFFrProject project = new ProjectLoader().load(proj);

    assertEquals(proj, project.getPath());
  }

  @Test
  void memoIsReadFromMarkerFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.writeString(proj.resolve(PROJECT_MARKER), "project memo text");

    AFFrProject project = new ProjectLoader().load(proj);

    assertEquals("project memo text", project.getMemo());
  }

  @Test
  void memoIsStrippedOfWhitespace(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.writeString(proj.resolve(PROJECT_MARKER), "  trimmed  \n");

    AFFrProject project = new ProjectLoader().load(proj);

    assertEquals("trimmed", project.getMemo());
  }

  @Test
  void emptyOrAbsentMarkerYieldsEmptyMemo(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));

    AFFrProject project = new ProjectLoader().load(proj);

    assertEquals("", project.getMemo());
  }

  // ── Item discovery ────────────────────────────────────────────────────────

  @Test
  void projectWithNoSubdirsHasEmptyItemList(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(PROJECT_MARKER));

    AFFrProject project = new ProjectLoader().load(proj);

    assertTrue(project.getItems().isEmpty());
  }

  @Test
  void subdirWithCalPropertyBecomesAFFrCalculation(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(PROJECT_MARKER));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{\"status\":\"SETTING\",\"date\":\"\"}");

    AFFrProject project = new ProjectLoader().load(proj);

    assertEquals(1, project.getItems().size());
    assertInstanceOf(AFFrCalculation.class, project.getItems().get(0));
    assertEquals("cal_01", project.getItems().get(0).name());
  }

  @Test
  void subdirWithoutCalPropertyIsIgnored(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(PROJECT_MARKER));
    Files.createDirectory(proj.resolve("not_a_cal"));

    AFFrProject project = new ProjectLoader().load(proj);

    assertTrue(project.getItems().isEmpty());
  }

  @Test
  void multipleCalculationsAreLoadedAlphabetically(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(PROJECT_MARKER));
    for (String name : new String[] {"cal_03", "cal_01", "cal_02"}) {
      Path cal = Files.createDirectory(proj.resolve(name));
      Files.writeString(cal.resolve(CAL_PROPERTY), "{}");
    }

    List<ProjectItem> items = new ProjectLoader().load(proj).getItems();

    assertEquals(3, items.size());
    assertEquals("cal_01", items.get(0).name());
    assertEquals("cal_02", items.get(1).name());
    assertEquals("cal_03", items.get(2).name());
  }

  // ── Back-reference ────────────────────────────────────────────────────────

  @Test
  void calculationHasBackReferenceToProject(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");

    AFFrProject project = new ProjectLoader().load(proj);
    AFFrCalculation c = (AFFrCalculation) project.getItems().get(0);

    assertNotNull(c.getProject());
    assertEquals(project, c.getProject());
  }

  // ── AFFrCalProperty fields ─────────────────────────────────────────────────

  @Test
  void statusIsReadFromCalProperty(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(
        cal.resolve(CAL_PROPERTY), "{\"status\":\"CALCULATED\",\"date\":\"2026-05-01\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(CalculationStatus.CALCULATED, c.getStatus());
  }

  @Test
  void unknownStatusFallsBackToSetting(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{\"status\":\"UNKNOWN_FUTURE_STATE\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(CalculationStatus.SETTING, c.getStatus());
  }

  @Test
  void missingStatusFallsBackToSetting(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(CalculationStatus.SETTING, c.getStatus());
  }

  @Test
  void malformedJsonFallsBackToDefaults(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "NOT JSON");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(CalculationStatus.SETTING, c.getStatus());
    assertEquals("", c.date());
  }

  @Test
  void dateIsReadFromCalProperty(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{\"date\":\"2026-04-15\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals("2026-04-15", c.date());
  }

  @Test
  void missingDateYieldsEmptyString(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals("", c.date());
  }

  @Test
  void ncpuIsReadFromCalProperty(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{\"ncpu\":4}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(4, c.getProperty().ncpu());
  }

  @Test
  void hostIsReadFromCalProperty(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{\"host\":\"hpc-server\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals("hpc-server", c.getProperty().host());
  }

  // ── AFFrCalculationModel (.mode) ──────────────────────────────────────────

  @Test
  void absentModeFileYieldsDefaultModel(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(AFFrCalculationModel.DEFAULT, c.getModel());
  }

  @Test
  void comprsModelIsReadFromModeFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");
    Files.writeString(cal.resolve(CAL_MODE), "{\"comprsModel\":\"COMPRESSIBLE\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(ComprsModel.COMPRESSIBLE, c.getModel().comprsModel());
  }

  @Test
  void steadyModelIsReadFromModeFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");
    Files.writeString(cal.resolve(CAL_MODE), "{\"steadyModel\":\"UNSTEADY\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(SteadyModel.UNSTEADY, c.getModel().steadyModel());
  }

  @Test
  void turbModelIsReadFromModeFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");
    Files.writeString(cal.resolve(CAL_MODE), "{\"turbModel\":\"LES\"}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(TurbModel.LES, c.getModel().turbModel());
  }

  @Test
  void extraModelsAreReadFromModeFile(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");
    Files.writeString(cal.resolve(CAL_MODE), "{\"extraModelSet\":[\"VOF\",\"CAVITATION\"]}");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertTrue(c.getModel().extraModelSet().contains(ExtraModel.VOF));
    assertTrue(c.getModel().extraModelSet().contains(ExtraModel.CAVITATION));
    assertEquals(2, c.getModel().extraModelSet().size());
  }

  @Test
  void malformedModeFileFallsBackToDefaultModel(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Path cal = Files.createDirectory(proj.resolve("cal_01"));
    Files.writeString(cal.resolve(CAL_PROPERTY), "{}");
    Files.writeString(cal.resolve(CAL_MODE), "NOT JSON");

    AFFrCalculation c = (AFFrCalculation) new ProjectLoader().load(proj).getItems().get(0);

    assertEquals(AFFrCalculationModel.DEFAULT, c.getModel());
  }
}
