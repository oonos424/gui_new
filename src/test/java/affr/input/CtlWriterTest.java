package affr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.project.AFFrCalculationModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link CtlWriter}. */
final class CtlWriterTest {

  private AFFrInput input;

  @BeforeEach
  void setUp() {
    input = AFFrInput.createEmpty(AFFrCalculationModel.DEFAULT);
  }

  // ── Value formatting ───────────────────────────────────────────────────────

  @Test
  void formatInteger() {
    assertEquals("42", CtlWriter.formatValue(new AFFrInteger("n", 42)));
  }

  @Test
  void formatNegativeInteger() {
    assertEquals("-7", CtlWriter.formatValue(new AFFrInteger("n", -7)));
  }

  @Test
  void formatReal() {
    String formatted = CtlWriter.formatValue(new AFFrReal("dt", 1.5e-3));
    // Should contain 'E' notation (Java Double.toString style, uppercased)
    assertTrue(
        formatted.contains("E") || formatted.contains("."), "Expected decimal or E-notation");
  }

  @Test
  void formatCharacter() {
    assertEquals("'inlet'", CtlWriter.formatValue(new AFFrCharacter("name", "inlet")));
  }

  @Test
  void formatLogicalTrue() {
    assertEquals(".TRUE.", CtlWriter.formatValue(new AFFrLogical("f", true)));
  }

  @Test
  void formatLogicalFalse() {
    assertEquals(".FALSE.", CtlWriter.formatValue(new AFFrLogical("f", false)));
  }

  // ── Serialization — single namelist ───────────────────────────────────────

  @Test
  void serializeSingleNamelistBlock() {
    input
        .getSingle(NamelistNames.MODEL)
        .setValue("flow", new AFFrCharacter("flow", "incompressible"));
    String out = CtlWriter.serialize(input);
    assertTrue(out.contains("&MODEL"), "Expected &MODEL header");
    assertTrue(out.contains("flow = 'incompressible'"), "Expected field line");
    assertTrue(out.contains("/"), "Expected closing /");
  }

  @Test
  void emptyNamelistIsOmitted() {
    // All namelists start empty; none should appear in the output
    String out = CtlWriter.serialize(input);
    assertFalse(out.contains("&MODEL"), "Empty MODEL should be omitted");
  }

  @Test
  void guiNamelistIsAlwaysExcluded() {
    input.getSingle(NamelistNames.GUI).setValue("x", new AFFrInteger("x", 1));
    String out = CtlWriter.serialize(input);
    assertFalse(out.contains("&GUI"), "GUI namelist must be excluded from output");
  }

  @Test
  void singleNamelistsAppearsBeforeMulti() {
    input
        .getSingle(NamelistNames.MODEL)
        .setValue("flow", new AFFrCharacter("flow", "incompressible"));
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(boundary);
    boundary.addInstance("inlet");
    boundary.setValue("inlet", "boundary_name", new AFFrCharacter("boundary_name", "inlet"));

    String out = CtlWriter.serialize(input);
    int modelPos = out.indexOf("&MODEL");
    int boundaryPos = out.indexOf("&BOUNDARY");
    assertTrue(modelPos < boundaryPos, "&MODEL should appear before &BOUNDARY");
  }

  // ── Serialization — multi namelist ────────────────────────────────────────

  @Test
  void serializeMultiNamelistTwoInstances() {
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(boundary);
    boundary.addInstance("inlet");
    boundary.setValue("inlet", "boundary_name", new AFFrCharacter("boundary_name", "inlet"));
    boundary.addInstance("outlet");
    boundary.setValue("outlet", "boundary_name", new AFFrCharacter("boundary_name", "outlet"));

    String out = CtlWriter.serialize(input);
    assertEquals(2, countOccurrences(out, "&BOUNDARY"), "Expected two &BOUNDARY blocks");
    assertTrue(out.contains("'inlet'"), "Expected inlet value");
    assertTrue(out.contains("'outlet'"), "Expected outlet value");
  }

  @Test
  void multiInstanceWithNoValuesIsOmitted() {
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(boundary);
    boundary.addInstance("empty_instance"); // no values set

    String out = CtlWriter.serialize(input);
    assertFalse(out.contains("&BOUNDARY"), "Instance with no values should be omitted");
  }

  // ── Write to file ─────────────────────────────────────────────────────────

  @Test
  void writeCreatesFile(@TempDir Path tmp) throws IOException {
    input.getSingle(NamelistNames.MODEL).setValue("ncpu", new AFFrInteger("ncpu", 4));
    Path target = tmp.resolve("fflow.ctl");
    CtlWriter.write(input, target);
    assertTrue(Files.exists(target), "File should have been created");
    String content = Files.readString(target);
    assertTrue(content.contains("&MODEL"));
    assertTrue(content.contains("ncpu = 4"));
  }

  @Test
  void writeOverwritesExistingFile(@TempDir Path tmp) throws IOException {
    Path target = tmp.resolve("fflow.ctl");
    Files.writeString(target, "old content");

    input.getSingle(NamelistNames.MODEL).setValue("ncpu", new AFFrInteger("ncpu", 2));
    CtlWriter.write(input, target);

    String content = Files.readString(target);
    assertFalse(content.contains("old content"), "Old content should be replaced");
    assertTrue(content.contains("ncpu = 2"));
  }

  // ── Round-trip ────────────────────────────────────────────────────────────

  @Test
  void roundTripSingleNamelistPreservesValues() {
    input
        .getSingle(NamelistNames.MODEL)
        .setValue("flow", new AFFrCharacter("flow", "incompressible"));
    input.getSingle(NamelistNames.MODEL).setValue("ncpu", new AFFrInteger("ncpu", 8));
    input.getSingle(NamelistNames.DELTAT).setValue("dt", new AFFrReal("dt", 1.0e-4));
    input.getSingle(NamelistNames.FLAGS).setValue("active", new AFFrLogical("active", true));

    String serialized = CtlWriter.serialize(input);

    AFFrInput reloaded = AFFrInput.createEmpty(AFFrCalculationModel.DEFAULT);
    CtlParser.populate(reloaded, serialized);

    assertEquals(
        "incompressible",
        reloaded.getSingle(NamelistNames.MODEL).getValue("flow").getCharacterValue());
    assertEquals(8, reloaded.getSingle(NamelistNames.MODEL).getValue("ncpu").getIntegerValue());
    assertEquals(
        1.0e-4, reloaded.getSingle(NamelistNames.DELTAT).getValue("dt").getRealValue(), 1e-20);
    assertEquals(
        true, reloaded.getSingle(NamelistNames.FLAGS).getValue("active").getLogicalValue());
  }

  @Test
  void roundTripMultiNamelistPreservesInstances() {
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(boundary);
    boundary.addInstance("inlet");
    boundary.setValue("inlet", "boundary_name", new AFFrCharacter("boundary_name", "inlet"));
    boundary.setValue("inlet", "area", new AFFrReal("area", 0.5));
    boundary.addInstance("wall");
    boundary.setValue("wall", "boundary_name", new AFFrCharacter("boundary_name", "wall"));

    String serialized = CtlWriter.serialize(input);

    AFFrInput reloaded = AFFrInput.createEmpty(AFFrCalculationModel.DEFAULT);
    CtlParser.populate(reloaded, serialized);

    AFFrNamelistMulti rb = reloaded.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(rb);
    assertNotNull(rb.getData("inlet"));
    assertNotNull(rb.getData("wall"));
    assertEquals(0.5, rb.getValue("inlet", "area").getRealValue(), 1e-15);
  }

  // ── Snapshot / unsaved-change detection ───────────────────────────────────

  @Test
  void snapshotMatchesWhenUnchanged() {
    input.getSingle(NamelistNames.MODEL).setValue("ncpu", new AFFrInteger("ncpu", 4));
    String snap = CtlWriter.snapshot(input);
    assertFalse(CtlWriter.hasUnsavedChanges(input, snap));
  }

  @Test
  void snapshotDetectsChange() {
    input.getSingle(NamelistNames.MODEL).setValue("ncpu", new AFFrInteger("ncpu", 4));
    String snap = CtlWriter.snapshot(input);
    input.getSingle(NamelistNames.MODEL).setValue("ncpu", new AFFrInteger("ncpu", 8));
    assertTrue(CtlWriter.hasUnsavedChanges(input, snap));
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private static int countOccurrences(String text, String sub) {
    int count = 0;
    int idx = 0;
    while ((idx = text.indexOf(sub, idx)) >= 0) {
      count++;
      idx += sub.length();
    }
    return count;
  }
}
