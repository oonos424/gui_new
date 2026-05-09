package affr.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CtlReader}.
 *
 * <p>Each test writes a minimal Fortran namelist snippet to a temp file and verifies that {@link
 * CtlReader#read(Path)} maps it to the expected {@link AFFrCalculationModel}.
 */
final class CtlReaderTest {

  // ── Helpers ────────────────────────────────────────────────────────────────

  private static Path writeCtl(Path dir, String content) throws IOException {
    Path file = dir.resolve("fflow.ctl");
    Files.writeString(file, content);
    return file;
  }

  // ── Compressibility (flow) ─────────────────────────────────────────────────

  @Test
  void parsesIncompressibleFlow(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  trbmdl = 'ke',\n/\n");

    assertEquals(ComprsModel.INCOMPRESSIBLE, CtlReader.read(ctl).comprsModel());
  }

  @Test
  void parsesCompressibleFlow(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'compressible',\n  trbmdl = 'ke',\n/\n");

    assertEquals(ComprsModel.COMPRESSIBLE, CtlReader.read(ctl).comprsModel());
  }

  @Test
  void missingModelSectionDefaultsToIncompressible(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&time\n  flowcon = 1,\n/\n");

    assertEquals(ComprsModel.INCOMPRESSIBLE, CtlReader.read(ctl).comprsModel());
  }

  // ── Steady / unsteady (flowcon) ────────────────────────────────────────────

  @Test
  void flowcon1IsSteady(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n/\n&time\n  flowcon = 1,\n/\n");

    assertEquals(SteadyModel.STEADY, CtlReader.read(ctl).steadyModel());
  }

  @Test
  void flowcon2IsUnsteady(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n/\n&time\n  flowcon = 2,\n/\n");

    assertEquals(SteadyModel.UNSTEADY, CtlReader.read(ctl).steadyModel());
  }

  @Test
  void missingTimeSectionDefaultsToSteady(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n/\n");

    assertEquals(SteadyModel.STEADY, CtlReader.read(ctl).steadyModel());
  }

  // ── Turbulence model (trbmdl) ──────────────────────────────────────────────

  @Test
  void keIsMappedToRans(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'ke',\n/\n");

    assertEquals(TurbModel.RANS, CtlReader.read(ctl).turbModel());
  }

  @Test
  void unknownTrbmdlFallsToRans(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'custom_model',\n/\n");

    assertEquals(TurbModel.RANS, CtlReader.read(ctl).turbModel());
  }

  @Test
  void slesIsMappedToLes(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'sles',\n/\n");

    assertEquals(TurbModel.LES, CtlReader.read(ctl).turbModel());
  }

  @Test
  void dlesIsMappedToLes(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'dles',\n/\n");

    assertEquals(TurbModel.LES, CtlReader.read(ctl).turbModel());
  }

  @Test
  void waleIsMappedToLes(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'wale',\n/\n");

    assertEquals(TurbModel.LES, CtlReader.read(ctl).turbModel());
  }

  @Test
  void dnsIsMappedToDns(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'dns',\n/\n");

    assertEquals(TurbModel.DNS, CtlReader.read(ctl).turbModel());
  }

  @Test
  void noTurbMappedToNo(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = 'no',\n/\n");

    assertEquals(TurbModel.NO, CtlReader.read(ctl).turbModel());
  }

  // ── Extra models: namelist presence ───────────────────────────────────────

  @Test
  void vofSectionDetected(@TempDir Path tmp) throws IOException {
    Path ctl =
        writeCtl(
            tmp,
            "&model\n  flow = 'incompressible',\n/\n"
                + "&time\n  flowcon = 2,\n/\n"
                + "&vof\n  nphase = 2,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.VOF));
  }

  @Test
  void cavitationSectionDetected(@TempDir Path tmp) throws IOException {
    Path ctl =
        writeCtl(
            tmp,
            "&model\n  flow = 'incompressible',\n/\n"
                + "&cavitation\n  some_param = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.CAVITATION));
  }

  @Test
  void radiationSectionDetected(@TempDir Path tmp) throws IOException {
    Path ctl =
        writeCtl(tmp, "&model\n  flow = 'compressible',\n/\n" + "&radoption\n  mode = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.RADIATION));
  }

  @Test
  void combustionFlameletSectionDetected(@TempDir Path tmp) throws IOException {
    Path ctl =
        writeCtl(
            tmp,
            "&model\n  flow = 'compressible',\n/\n" + "&flamelet\n  table = 'file.tbl',\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.COMBUSTION));
  }

  // ── Extra models: model flags ──────────────────────────────────────────────

  @Test
  void particleTrackFlagDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  cal_particle = 2,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.PARTICLE_TRACK));
  }

  @Test
  void particleTrackZeroNotDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  cal_particle = 0,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().isEmpty());
  }

  @Test
  void movingMeshFlagDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  cal_mvmsh = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.MOVING_MESH));
  }

  @Test
  void oversetGridFlagDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  cal_ovset = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.OVERSET_GRID));
  }

  @Test
  void rotatingFrameFlagDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  multi_frame = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.ROTATING_FRAME));
  }

  @Test
  void combustChemReactFlagDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'compressible',\n  cal_reac = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.COMBUST_CHEM_REACT));
  }

  @Test
  void surfaceReactionFlagDetected(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'compressible',\n  cal_surf = 1,\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().contains(ExtraModel.SURFACE_REACTION));
  }

  // ── No extras ─────────────────────────────────────────────────────────────

  @Test
  void noExtraModelsWhenNoneFlagged(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = 'incompressible',\n  trbmdl = 'ke',\n/\n");

    assertTrue(CtlReader.read(ctl).extraModelSet().isEmpty());
  }

  // ── Full model round-trip ─────────────────────────────────────────────────

  @Test
  void fullCompressibleUnsteadyLesWithExtras(@TempDir Path tmp) throws IOException {
    Path ctl =
        writeCtl(
            tmp,
            "&model\n"
                + "  flow = 'compressible',\n"
                + "  trbmdl = 'sles',\n"
                + "  cal_mvmsh = 1,\n"
                + "/\n"
                + "&time\n"
                + "  flowcon = 2,\n"
                + "/\n"
                + "&radoption\n"
                + "  mode = 1,\n"
                + "/\n");

    AFFrCalculationModel model = CtlReader.read(ctl);

    assertEquals(ComprsModel.COMPRESSIBLE, model.comprsModel());
    assertEquals(SteadyModel.UNSTEADY, model.steadyModel());
    assertEquals(TurbModel.LES, model.turbModel());
    assertEquals(Set.of(ExtraModel.MOVING_MESH, ExtraModel.RADIATION), model.extraModelSet());
  }

  // ── Parsing robustness ────────────────────────────────────────────────────

  @Test
  void parsesDoubleQuotedStringValues(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  flow = \"incompressible\",\n  trbmdl = \"ke\",\n/\n");

    AFFrCalculationModel model = CtlReader.read(ctl);
    assertEquals(ComprsModel.INCOMPRESSIBLE, model.comprsModel());
    assertEquals(TurbModel.RANS, model.turbModel());
  }

  @Test
  void parsesUnquotedValues(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&model\n  trbmdl = dns,\n/\n");

    assertEquals(TurbModel.DNS, CtlReader.read(ctl).turbModel());
  }

  @Test
  void caseInsensitiveNamelistName(@TempDir Path tmp) throws IOException {
    Path ctl = writeCtl(tmp, "&MODEL\n  flow = 'compressible',\n/\n");

    assertEquals(ComprsModel.COMPRESSIBLE, CtlReader.read(ctl).comprsModel());
  }

  @Test
  void throwsOnNonExistentFile(@TempDir Path tmp) {
    Path missing = tmp.resolve("no_such_file.ctl");

    assertThrows(IOException.class, () -> CtlReader.read(missing));
  }
}
