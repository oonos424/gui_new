package affr.util.prefs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link UserPreferences}.
 *
 * <p>Each test uses a JUnit {@link TempDir} so that reads and writes go to an isolated temporary
 * directory and never touch {@code ~/.affr/}.
 */
final class UserPreferencesTest {

  // -------------------------------------------------------------------------
  // Defaults (no file present)
  // -------------------------------------------------------------------------

  @Test
  void defaultsApplyWhenNoFileExists(@TempDir Path dir) {
    UserPreferences prefs = UserPreferences.loadFrom(dir.resolve("prefs.properties"));

    assertEquals(Locale.ENGLISH, prefs.locale());
    assertEquals(UserPreferences.DEFAULT_WINDOW_WIDTH, prefs.windowWidth());
    assertEquals(UserPreferences.DEFAULT_WINDOW_HEIGHT, prefs.windowHeight());
    assertFalse(Double.isFinite(prefs.windowX()), "windowX should be NaN when not saved");
    assertFalse(Double.isFinite(prefs.windowY()), "windowY should be NaN when not saved");
    assertNull(prefs.browserViewMode(), "browserViewMode should be null when not saved");
    assertNull(prefs.browserPath(), "browserPath should be null when not saved");
    // Install paths fall back to platform defaults (non-null on Windows, null elsewhere).
    assertEquals(UserPreferences.defaultGuiInstallPath(), prefs.guiInstallPath());
    assertEquals(UserPreferences.defaultSolverInstallPath(), prefs.solverInstallPath());
  }

  // -------------------------------------------------------------------------
  // Language round-trip
  // -------------------------------------------------------------------------

  @Test
  void languageRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setLocale(Locale.JAPANESE);
    prefs.save();

    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertEquals(Locale.JAPANESE, loaded.locale());
  }

  @Test
  void unknownLanguageTagFallsBackToEnglish(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "language=zz\n");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    assertEquals(Locale.ENGLISH, prefs.locale());
  }

  // -------------------------------------------------------------------------
  // Window size round-trip
  // -------------------------------------------------------------------------

  @Test
  void windowSizeRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setWindowSize(1440, 900);
    prefs.save();

    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertEquals(1440.0, loaded.windowWidth());
    assertEquals(900.0, loaded.windowHeight());
  }

  @Test
  void invalidWindowSizeFallsBackToDefault(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "window.width=-100\nwindow.height=abc\n");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    assertEquals(UserPreferences.DEFAULT_WINDOW_WIDTH, prefs.windowWidth());
    assertEquals(UserPreferences.DEFAULT_WINDOW_HEIGHT, prefs.windowHeight());
  }

  // -------------------------------------------------------------------------
  // Window position round-trip
  // -------------------------------------------------------------------------

  @Test
  void windowPositionRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setWindowPosition(200, 150);
    prefs.save();

    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertEquals(200.0, loaded.windowX());
    assertEquals(150.0, loaded.windowY());
  }

  @Test
  void negativePositionIsPreserved(@TempDir Path dir) {
    // Negative coordinates are valid on multi-monitor setups where a secondary
    // monitor is to the left of or above the primary.
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setWindowPosition(-800, -50);
    prefs.save();

    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertEquals(-800.0, loaded.windowX());
    assertEquals(-50.0, loaded.windowY());
  }

  @Test
  void unsavedPositionIsNaN(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setWindowSize(1200, 720);
    prefs.save();

    // Position was never set, so it must not appear in the file.
    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertFalse(Double.isFinite(loaded.windowX()), "windowX should be NaN");
    assertFalse(Double.isFinite(loaded.windowY()), "windowY should be NaN");
  }

  // -------------------------------------------------------------------------
  // Browser view mode round-trip
  // -------------------------------------------------------------------------

  @Test
  void browserViewModeRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setBrowserViewMode("TREE");
    prefs.save();

    assertEquals("TREE", UserPreferences.loadFrom(file).browserViewMode());
  }

  @Test
  void allViewModeNamesRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    for (String mode : List.of("LIST", "ICON", "TREE")) {
      UserPreferences prefs = UserPreferences.loadFrom(file);
      prefs.setBrowserViewMode(mode);
      prefs.save();
      assertEquals(mode, UserPreferences.loadFrom(file).browserViewMode(), "failed for: " + mode);
    }
  }

  @Test
  void unsavedBrowserViewModeIsNull(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setWindowSize(800, 600); // save something else but not the view mode
    prefs.save();

    assertNull(UserPreferences.loadFrom(file).browserViewMode());
  }

  // -------------------------------------------------------------------------
  // Browser path round-trip
  // -------------------------------------------------------------------------

  @Test
  void browserPathRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");
    Path savedPath = dir.resolve("workspace/subdir");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setBrowserPath(savedPath);
    prefs.save();

    assertEquals(savedPath, UserPreferences.loadFrom(file).browserPath());
  }

  @Test
  void corruptBrowserPathYieldsNull(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    // NUL is illegal in path names on every supported platform; Path.of() throws
    // InvalidPathException, which loadFrom() must swallow rather than propagate.
    java.nio.file.Files.writeString(file, "browser.path=" + "\u0000" + "invalid\n");

    assertNull(UserPreferences.loadFrom(file).browserPath());
  }

  @Test
  void unsavedBrowserPathIsNull(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setWindowSize(800, 600); // save something else but not the path
    prefs.save();

    assertNull(UserPreferences.loadFrom(file).browserPath());
  }

  // -------------------------------------------------------------------------
  // Full round-trip
  // -------------------------------------------------------------------------

  @Test
  void allFieldsRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");
    Path savedPath = dir.resolve("workspace");
    Path guiPath = dir.resolve("gui");
    Path solverPath = dir.resolve("solver");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setLocale(Locale.JAPANESE);
    prefs.setWindowSize(1920, 1080);
    prefs.setWindowPosition(100, 50);
    prefs.setBrowserViewMode("ICON");
    prefs.setBrowserPath(savedPath);
    prefs.setGuiInstallPath(guiPath);
    prefs.setSolverInstallPath(solverPath);
    prefs.save();

    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertEquals(Locale.JAPANESE, loaded.locale());
    assertEquals(1920.0, loaded.windowWidth());
    assertEquals(1080.0, loaded.windowHeight());
    assertEquals(100.0, loaded.windowX());
    assertEquals(50.0, loaded.windowY());
    assertEquals("ICON", loaded.browserViewMode());
    assertEquals(savedPath, loaded.browserPath());
    assertEquals(guiPath, loaded.guiInstallPath());
    assertEquals(solverPath, loaded.solverInstallPath());
  }

  @Test
  void saveIsIdempotent(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setLocale(Locale.JAPANESE);
    prefs.setWindowSize(1280, 800);
    prefs.setWindowPosition(300, 200);
    prefs.save();
    prefs.save();

    UserPreferences loaded = UserPreferences.loadFrom(file);
    assertEquals(Locale.JAPANESE, loaded.locale());
    assertEquals(1280.0, loaded.windowWidth());
    assertEquals(800.0, loaded.windowHeight());
    assertEquals(300.0, loaded.windowX());
    assertEquals(200.0, loaded.windowY());
  }

  // -------------------------------------------------------------------------
  // Missing / corrupt file
  // -------------------------------------------------------------------------

  @Test
  void emptyFileYieldsDefaults(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    assertEquals(Locale.ENGLISH, prefs.locale());
    assertEquals(UserPreferences.DEFAULT_WINDOW_WIDTH, prefs.windowWidth());
    assertEquals(UserPreferences.DEFAULT_WINDOW_HEIGHT, prefs.windowHeight());
    assertFalse(Double.isFinite(prefs.windowX()));
    assertFalse(Double.isFinite(prefs.windowY()));
    assertNull(prefs.browserViewMode());
    assertNull(prefs.browserPath());
    assertEquals(UserPreferences.defaultGuiInstallPath(), prefs.guiInstallPath());
    assertEquals(UserPreferences.defaultSolverInstallPath(), prefs.solverInstallPath());
  }

  @Test
  void saveCreatesParentDirectoryIfAbsent(@TempDir Path dir) {
    Path file = dir.resolve("nested/subdir/prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.save();

    assertTrue(java.nio.file.Files.exists(file), "prefs file should have been created");
  }

  // -------------------------------------------------------------------------
  // GUI install path
  // -------------------------------------------------------------------------

  @Test
  void guiInstallPathRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");
    Path guiPath = dir.resolve("AFFrGUI");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setGuiInstallPath(guiPath);
    prefs.save();

    assertEquals(guiPath, UserPreferences.loadFrom(file).guiInstallPath());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void guiInstallPathDefaultIsWindowsInstallDirOnWindows(@TempDir Path dir) {
    UserPreferences prefs = UserPreferences.loadFrom(dir.resolve("prefs.properties"));

    assertEquals(Path.of("C:\\Program Files\\Advancesoft\\AFFrGUI"), prefs.guiInstallPath());
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void guiInstallPathDefaultIsNullOnNonWindows(@TempDir Path dir) {
    UserPreferences prefs = UserPreferences.loadFrom(dir.resolve("prefs.properties"));

    assertNull(prefs.guiInstallPath());
  }

  @Test
  void explicitGuiInstallPathOverridesPlatformDefault(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");
    Path customPath = dir.resolve("custom_gui");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setGuiInstallPath(customPath);
    prefs.save();

    // Must come back as the explicit path regardless of platform default.
    assertEquals(customPath, UserPreferences.loadFrom(file).guiInstallPath());
  }

  @Test
  void corruptGuiInstallPathFallsBackToPlatformDefault(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "gui.installPath=\u0000invalid\n");

    assertEquals(
        UserPreferences.defaultGuiInstallPath(), UserPreferences.loadFrom(file).guiInstallPath());
  }

  @Test
  void blankGuiInstallPathFallsBackToPlatformDefault(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "gui.installPath=   \n");

    assertEquals(
        UserPreferences.defaultGuiInstallPath(), UserPreferences.loadFrom(file).guiInstallPath());
  }

  @Test
  void settingGuiInstallPathToNullRestoresPlatformDefaultAfterReload(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setGuiInstallPath(null);
    prefs.save();

    // Key is absent from file; reload must return the platform default (null on non-Windows).
    assertEquals(
        UserPreferences.defaultGuiInstallPath(), UserPreferences.loadFrom(file).guiInstallPath());
  }

  // -------------------------------------------------------------------------
  // Solver install path
  // -------------------------------------------------------------------------

  @Test
  void solverInstallPathRoundTrip(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");
    Path solverPath = dir.resolve("AFFr");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setSolverInstallPath(solverPath);
    prefs.save();

    assertEquals(solverPath, UserPreferences.loadFrom(file).solverInstallPath());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void solverInstallPathDefaultIsWindowsInstallDirOnWindows(@TempDir Path dir) {
    UserPreferences prefs = UserPreferences.loadFrom(dir.resolve("prefs.properties"));

    assertEquals(Path.of("C:\\Program Files\\Advancesoft\\AFFr"), prefs.solverInstallPath());
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void solverInstallPathDefaultIsNullOnNonWindows(@TempDir Path dir) {
    UserPreferences prefs = UserPreferences.loadFrom(dir.resolve("prefs.properties"));

    assertNull(prefs.solverInstallPath());
  }

  @Test
  void explicitSolverInstallPathOverridesPlatformDefault(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");
    Path customPath = dir.resolve("custom_solver");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setSolverInstallPath(customPath);
    prefs.save();

    assertEquals(customPath, UserPreferences.loadFrom(file).solverInstallPath());
  }

  @Test
  void corruptSolverInstallPathFallsBackToPlatformDefault(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "solver.installPath=\u0000invalid\n");

    assertEquals(
        UserPreferences.defaultSolverInstallPath(),
        UserPreferences.loadFrom(file).solverInstallPath());
  }

  @Test
  void blankSolverInstallPathFallsBackToPlatformDefault(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prefs.properties");
    java.nio.file.Files.writeString(file, "solver.installPath=   \n");

    assertEquals(
        UserPreferences.defaultSolverInstallPath(),
        UserPreferences.loadFrom(file).solverInstallPath());
  }

  @Test
  void settingSolverInstallPathToNullRestoresPlatformDefaultAfterReload(@TempDir Path dir) {
    Path file = dir.resolve("prefs.properties");

    UserPreferences prefs = UserPreferences.loadFrom(file);
    prefs.setSolverInstallPath(null);
    prefs.save();

    assertEquals(
        UserPreferences.defaultSolverInstallPath(),
        UserPreferences.loadFrom(file).solverInstallPath());
  }
}
