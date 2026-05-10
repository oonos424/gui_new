package affr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import affr.project.AFFrCalculationModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link CtlParser}. */
final class CtlParserTest {

  private AFFrInput input;

  @BeforeEach
  void setUp() {
    input = AFFrInput.createEmpty(AFFrCalculationModel.DEFAULT);
  }

  // ── Single-instance namelist ───────────────────────────────────────────────

  @Test
  void parseSingleNamelistInteger() {
    CtlParser.populate(input, "&MODEL\n  ncpu = 4\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.MODEL).getValue("ncpu");
    assertNotNull(v);
    assertInstanceOf(AFFrInteger.class, v);
    assertEquals(4, v.getIntegerValue());
  }

  @Test
  void parseSingleNamelistReal() {
    CtlParser.populate(input, "&DELTAT\n  dt = 1.0E-3\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.DELTAT).getValue("dt");
    assertNotNull(v);
    assertInstanceOf(AFFrReal.class, v);
    assertEquals(1.0e-3, v.getRealValue(), 1e-20);
  }

  @Test
  void parseSingleNamelistCharacterSingleQuoted() {
    CtlParser.populate(input, "&MODEL\n  flow = 'incompressible'\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.MODEL).getValue("flow");
    assertNotNull(v);
    assertInstanceOf(AFFrCharacter.class, v);
    assertEquals("incompressible", v.getCharacterValue());
  }

  @Test
  void parseSingleNamelistCharacterDoubleQuoted() {
    CtlParser.populate(input, "&MODEL\n  trbmdl = \"ke\"\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.MODEL).getValue("trbmdl");
    assertNotNull(v);
    assertEquals("ke", v.getCharacterValue());
  }

  @Test
  void parseSingleNamelistLogicalTrue() {
    CtlParser.populate(input, "&FLAGS\n  flag_a = .TRUE.\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.FLAGS).getValue("flag_a");
    assertNotNull(v);
    assertInstanceOf(AFFrLogical.class, v);
    assertEquals(true, v.getLogicalValue());
  }

  @Test
  void parseSingleNamelistLogicalFalse() {
    CtlParser.populate(input, "&FLAGS\n  flag_b = .FALSE.\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.FLAGS).getValue("flag_b");
    assertNotNull(v);
    assertEquals(false, v.getLogicalValue());
  }

  @Test
  void parseSingleNamelistShortLogicalT() {
    CtlParser.populate(input, "&FLAGS\n  active = T\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.FLAGS).getValue("active");
    assertNotNull(v);
    assertEquals(true, v.getLogicalValue());
  }

  @Test
  void parseSingleNamelistShortLogicalF() {
    CtlParser.populate(input, "&FLAGS\n  active = F\n/\n");
    AFFrValue v = input.getSingle(NamelistNames.FLAGS).getValue("active");
    assertNotNull(v);
    assertEquals(false, v.getLogicalValue());
  }

  @Test
  void parseSingleNamelistMultipleFields() {
    CtlParser.populate(input, "&MODEL\n  flow = 'incompressible', ncpu = 8\n/\n");
    assertEquals(
        "incompressible",
        input.getSingle(NamelistNames.MODEL).getValue("flow").getCharacterValue());
    assertEquals(8, input.getSingle(NamelistNames.MODEL).getValue("ncpu").getIntegerValue());
  }

  @Test
  void parseCaseInsensitiveNamelistName() {
    CtlParser.populate(input, "&model\n  ncpu = 2\n/\n");
    assertNotNull(input.getSingle(NamelistNames.MODEL).getValue("ncpu"));
  }

  @Test
  void parseCaseInsensitiveFieldName() {
    CtlParser.populate(input, "&MODEL\n  FLOW = 'compressible'\n/\n");
    // Keys are stored lower-case
    assertNotNull(input.getSingle(NamelistNames.MODEL).getValue("flow"));
  }

  // ── Multi-instance namelist ────────────────────────────────────────────────

  @Test
  void parseMultiNamelistCreatesInstance() {
    CtlParser.populate(input, "&BOUNDARY\n  boundary_name = 'inlet', boundary_type = 'inlet'\n/\n");
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(boundary);
    assertNotNull(boundary.getData("inlet"));
  }

  @Test
  void parseMultiNamelistSetsFieldOnInstance() {
    CtlParser.populate(
        input, "&BOUNDARY\n  boundary_name = 'outlet', boundary_type = 'outlet'\n/\n");
    AFFrValue v = input.getMulti(NamelistNames.BOUNDARY).getValue("outlet", "boundary_type");
    assertNotNull(v);
    assertEquals("outlet", v.getCharacterValue());
  }

  @Test
  void parseMultiNamelistTwoInstances() {
    String ctl =
        "&BOUNDARY\n  boundary_name = 'inlet', boundary_type = 'inlet'\n/\n"
            + "&BOUNDARY\n  boundary_name = 'outlet', boundary_type = 'outlet'\n/\n";
    CtlParser.populate(input, ctl);
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    assertNotNull(boundary.getData("inlet"));
    assertNotNull(boundary.getData("outlet"));
    assertEquals("inlet", boundary.getValue("inlet", "boundary_type").getCharacterValue());
    assertEquals("outlet", boundary.getValue("outlet", "boundary_type").getCharacterValue());
  }

  @Test
  void parseMultiNamelistNoKeyVariableFallsBackToAutoKey() {
    // BOUNDARY block with no boundary_name field
    CtlParser.populate(input, "&BOUNDARY\n  boundary_type = 'wall'\n/\n");
    AFFrNamelistMulti boundary = input.getMulti(NamelistNames.BOUNDARY);
    // Should have created one instance with an auto-generated key
    assertEquals(1, boundary.getInstanceKeyList().size());
  }

  // ── Unknown namelists ──────────────────────────────────────────────────────

  @Test
  void unknownNamelistIsIgnored() {
    CtlParser.populate(input, "&UNKNOWN_NML\n  x = 1\n/\n");
    assertNull(input.getNamelist("UNKNOWN_NML"));
  }

  // ── Type inference edge cases ──────────────────────────────────────────────

  @Test
  void inferTypeInteger() {
    AFFrValue v = CtlParser.inferType("n", "42");
    assertInstanceOf(AFFrInteger.class, v);
    assertEquals(42, v.getIntegerValue());
  }

  @Test
  void inferTypeNegativeInteger() {
    AFFrValue v = CtlParser.inferType("n", "-5");
    assertInstanceOf(AFFrInteger.class, v);
    assertEquals(-5, v.getIntegerValue());
  }

  @Test
  void inferTypeReal() {
    AFFrValue v = CtlParser.inferType("x", "3.14");
    assertInstanceOf(AFFrReal.class, v);
    assertEquals(3.14, v.getRealValue(), 1e-10);
  }

  @Test
  void inferTypeRealScientific() {
    AFFrValue v = CtlParser.inferType("dt", "1.0E-6");
    assertInstanceOf(AFFrReal.class, v);
    assertEquals(1.0e-6, v.getRealValue(), 1e-20);
  }

  @Test
  void inferTypeLogicalDotTrue() {
    AFFrValue v = CtlParser.inferType("f", ".TRUE.");
    assertInstanceOf(AFFrLogical.class, v);
    assertEquals(true, v.getLogicalValue());
  }

  @Test
  void inferTypeLogicalLowerCase() {
    AFFrValue v = CtlParser.inferType("f", ".false.");
    assertInstanceOf(AFFrLogical.class, v);
    assertEquals(false, v.getLogicalValue());
  }

  @Test
  void inferTypeCharacterQuoted() {
    AFFrValue v = CtlParser.inferType("s", "'hello'");
    assertInstanceOf(AFFrCharacter.class, v);
    assertEquals("hello", v.getCharacterValue());
  }

  @Test
  void inferTypeCharacterUnquotedFallback() {
    AFFrValue v = CtlParser.inferType("s", "someword");
    assertInstanceOf(AFFrCharacter.class, v);
    assertEquals("someword", v.getCharacterValue());
  }

  @Test
  void inferTypeFortranDExponent() {
    AFFrValue v = CtlParser.inferType("x", "1.0D-3");
    assertInstanceOf(AFFrReal.class, v);
    assertEquals(1.0e-3, v.getRealValue(), 1e-20);
  }

  @Test
  void inferTypeNegativeReal() {
    AFFrValue v = CtlParser.inferType("x", "-3.14");
    assertInstanceOf(AFFrReal.class, v);
    assertEquals(-3.14, v.getRealValue(), 1e-10);
  }

  @Test
  void inferTypeIntegerOverflow() {
    AFFrValue v = CtlParser.inferType("n", "2147483648");
    assertInstanceOf(AFFrCharacter.class, v);
    assertEquals("2147483648", v.getCharacterValue());
  }

  // ── Integration: loadFromFile ──────────────────────────────────────────────

  @Test
  void loadFromFilePopulatesValues(@TempDir Path tmp) throws IOException {
    Path ctlFile = tmp.resolve("fflow.ctl");
    Files.writeString(ctlFile, "&MODEL\n  ncpu = 8\n/\n");

    AFFrInput loaded = AFFrInput.loadFromFile(ctlFile, AFFrCalculationModel.DEFAULT);

    AFFrValue v = loaded.getSingle(NamelistNames.MODEL).getValue("ncpu");
    assertNotNull(v);
    assertInstanceOf(AFFrInteger.class, v);
    assertEquals(8, v.getIntegerValue());
  }

  // ── Multiple namelists in one file ─────────────────────────────────────────

  @Test
  void parseMultipleNamelistsInOneFile() {
    String ctl = "&MODEL\n  flow = 'incompressible'\n/\n" + "&TIME\n  flowcon = 1\n/\n";
    CtlParser.populate(input, ctl);
    assertNotNull(input.getSingle(NamelistNames.MODEL).getValue("flow"));
    assertNotNull(input.getSingle(NamelistNames.TIME).getValue("flowcon"));
  }
}
