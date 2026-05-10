package affr.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the package-private helper {@link SettingsDialog#parsePath(String)}.
 *
 * <p>Dialog interaction (pre-fill, OK commit, Cancel discard) is covered by the round-trip
 * behaviour exercised through {@link affr.util.prefs.UserPreferencesTest}. Tests here focus on the
 * path-parsing logic in isolation, which runs on every OK click.
 */
final class SettingsDialogTest {

  @Test
  void parsePathReturnsNullForEmptyString() {
    assertNull(SettingsDialog.parsePath(""));
  }

  @Test
  void parsePathReturnsNullForBlankString() {
    assertNull(SettingsDialog.parsePath("   "));
  }

  @Test
  void parsePathReturnsParsedPathForValidString() {
    Path expected = Path.of("C:\\Program Files\\Advancesoft\\AFFrGUI");
    assertEquals(expected, SettingsDialog.parsePath("C:\\Program Files\\Advancesoft\\AFFrGUI"));
  }

  @Test
  void parsePathTrimsWhitespaceBeforeParsing() {
    Path expected = Path.of("C:\\some\\dir");
    assertEquals(expected, SettingsDialog.parsePath("  C:\\some\\dir  "));
  }

  @Test
  @org.junit.jupiter.api.condition.EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
  void parsePathReturnsNullForWindowsIllegalPathCharacters() {
    // Asterisk is forbidden in Windows file names; WindowsPath throws InvalidPathException.
    assertNull(SettingsDialog.parsePath("C:\\some\\fo*lder"));
  }
}
