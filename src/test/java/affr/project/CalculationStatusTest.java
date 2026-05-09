package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CalculationStatus}.
 *
 * <p>These tests verify the structural contracts of the enum — value count, message-key format —
 * without requiring I18n initialisation (no {@code label()} calls).
 */
final class CalculationStatusTest {

  @Test
  void sevenStatesAreDefined() {
    assertEquals(7, CalculationStatus.values().length);
  }

  @Test
  void everyValueHasNonBlankMessageKey() {
    for (CalculationStatus s : CalculationStatus.values()) {
      assertNotNull(s.messageKey(), s.name() + " has null messageKey");
      assertFalse(s.messageKey().isBlank(), s.name() + " has blank messageKey");
    }
  }

  @Test
  void messageKeysFollowDotNotation() {
    for (CalculationStatus s : CalculationStatus.values()) {
      assertFalse(
          s.messageKey().startsWith("."), s.name() + " messageKey must not start with a dot");
      assertFalse(s.messageKey().endsWith("."), s.name() + " messageKey must not end with a dot");
    }
  }

  @Test
  void expectedEnumNamesExist() {
    // Verify the exact names the spec documents so that renames do not silently break behaviour.
    assertNotNull(CalculationStatus.valueOf("SETTING"));
    assertNotNull(CalculationStatus.valueOf("SETUP"));
    assertNotNull(CalculationStatus.valueOf("QUEUING"));
    assertNotNull(CalculationStatus.valueOf("CALCULATING"));
    assertNotNull(CalculationStatus.valueOf("CALCULATED"));
    assertNotNull(CalculationStatus.valueOf("CAL_ABORTED"));
    assertNotNull(CalculationStatus.valueOf("PRE_ABORTED"));
  }
}
