package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrCalProperty}. */
final class AFFrCalPropertyTest {

  // ── DEFAULT constant ───────────────────────────────────────────────────────

  @Test
  void defaultStatusIsSetting() {
    assertEquals(CalculationStatus.SETTING, AFFrCalProperty.DEFAULT.status());
  }

  @Test
  void defaultDateIsEmpty() {
    assertEquals("", AFFrCalProperty.DEFAULT.date());
  }

  @Test
  void defaultTimeStepIsZero() {
    assertEquals(0, AFFrCalProperty.DEFAULT.timeStep());
  }

  @Test
  void defaultHostIsLocalhost() {
    assertEquals("localhost", AFFrCalProperty.DEFAULT.host());
  }

  @Test
  void defaultJobIdIsEmpty() {
    assertEquals("", AFFrCalProperty.DEFAULT.jobId());
  }

  @Test
  void defaultNcpuIsOne() {
    assertEquals(1, AFFrCalProperty.DEFAULT.ncpu());
  }

  @Test
  void defaultUserSubrtUsedIsFalse() {
    assertFalse(AFFrCalProperty.DEFAULT.userSubrtUsed());
  }

  @Test
  void defaultExecFilesIsEmpty() {
    assertTrue(AFFrCalProperty.DEFAULT.execFiles().isEmpty());
  }

  @Test
  void defaultUsrsubCheckIsEmpty() {
    assertTrue(AFFrCalProperty.DEFAULT.usrsubCheck().isEmpty());
  }

  // ── Record construction ────────────────────────────────────────────────────

  @Test
  void constructorPreservesAllFields() {
    Map<String, String> execFiles = Map.of("fflow", "/usr/bin/fflow");
    Map<String, Boolean> usrsubCheck = Map.of("sub1", true);

    AFFrCalProperty p =
        new AFFrCalProperty(
            CalculationStatus.CALCULATED,
            "2026-05-01",
            500,
            "hpc-server",
            "job-42",
            "queue-A",
            8,
            true,
            execFiles,
            usrsubCheck);

    assertEquals(CalculationStatus.CALCULATED, p.status());
    assertEquals("2026-05-01", p.date());
    assertEquals(500, p.timeStep());
    assertEquals("hpc-server", p.host());
    assertEquals("job-42", p.jobId());
    assertEquals("queue-A", p.queueName());
    assertEquals(8, p.ncpu());
    assertTrue(p.userSubrtUsed());
    assertEquals(execFiles, p.execFiles());
    assertEquals(usrsubCheck, p.usrsubCheck());
  }

  // ── withStatus ─────────────────────────────────────────────────────────────

  @Test
  void withStatusChangesOnlyStatus() {
    Map<String, String> execFiles = Map.of("fflow", "/usr/bin/fflow");
    Map<String, Boolean> usrsubCheck = Map.of("sub1", true);

    AFFrCalProperty original =
        new AFFrCalProperty(
            CalculationStatus.CALCULATED,
            "2026-05-01",
            500,
            "hpc-server",
            "job-42",
            "queue-A",
            8,
            true,
            execFiles,
            usrsubCheck);

    AFFrCalProperty result = original.withStatus(CalculationStatus.SETTING);

    assertEquals(CalculationStatus.SETTING, result.status());
    assertEquals(original.date(), result.date());
    assertEquals(original.timeStep(), result.timeStep());
    assertEquals(original.host(), result.host());
    assertEquals(original.jobId(), result.jobId());
    assertEquals(original.queueName(), result.queueName());
    assertEquals(original.ncpu(), result.ncpu());
    assertEquals(original.userSubrtUsed(), result.userSubrtUsed());
    assertEquals(original.execFiles(), result.execFiles());
    assertEquals(original.usrsubCheck(), result.usrsubCheck());
  }

  @Test
  void withStatusDoesNotMutateOriginal() {
    AFFrCalProperty original = AFFrCalProperty.DEFAULT;
    original.withStatus(CalculationStatus.CALCULATED);
    assertEquals(CalculationStatus.SETTING, original.status());
  }
}
