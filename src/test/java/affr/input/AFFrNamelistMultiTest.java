package affr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrNamelistMulti}. */
final class AFFrNamelistMultiTest {

  private AFFrNamelistMulti namelist;

  @BeforeEach
  void setUp() {
    namelist = new AFFrNamelistMulti("BOUNDARY");
  }

  // ── addInstance ────────────────────────────────────────────────────────────

  @Test
  void addInstanceCreatesDataForKey() {
    namelist.addInstance("inlet");
    assertNotNull(namelist.getData("inlet"));
  }

  @Test
  void addInstanceReturnsTrueForNewKey() {
    assertTrue(namelist.addInstance("inlet"));
  }

  @Test
  void addInstanceReturnsFalseForDuplicateKey() {
    namelist.addInstance("inlet");
    assertFalse(namelist.addInstance("inlet"));
  }

  @Test
  void addInstanceDuplicateDoesNotReplaceExistingData() {
    namelist.addInstance("inlet");
    namelist.setValue("inlet", "boundary_type", new AFFrCharacter("boundary_type", "wall"));
    namelist.addInstance("inlet");
    assertEquals("wall", namelist.getValue("inlet", "boundary_type").getCharacterValue());
  }

  // ── removeInstance ─────────────────────────────────────────────────────────

  @Test
  void removeInstanceRemovesData() {
    namelist.addInstance("inlet");
    namelist.removeInstance("inlet");
    assertNull(namelist.getData("inlet"));
  }

  @Test
  void removeInstanceReturnsTrueWhenPresent() {
    namelist.addInstance("inlet");
    assertTrue(namelist.removeInstance("inlet"));
  }

  @Test
  void removeInstanceReturnsFalseWhenAbsent() {
    assertFalse(namelist.removeInstance("no_such_key"));
  }

  // ── Per-instance value isolation ───────────────────────────────────────────

  @Test
  void valuesAreIsolatedBetweenInstances() {
    namelist.addInstance("inlet");
    namelist.addInstance("outlet");

    namelist.setValue("inlet", "boundary_type", new AFFrCharacter("boundary_type", "inlet"));
    namelist.setValue("outlet", "boundary_type", new AFFrCharacter("boundary_type", "outlet"));

    assertEquals("inlet", namelist.getValue("inlet", "boundary_type").getCharacterValue());
    assertEquals("outlet", namelist.getValue("outlet", "boundary_type").getCharacterValue());
  }

  @Test
  void removeValueInOneInstanceDoesNotAffectAnother() {
    namelist.addInstance("inlet");
    namelist.addInstance("outlet");

    namelist.setValue("inlet", "area", new AFFrReal("area", 1.0));
    namelist.setValue("outlet", "area", new AFFrReal("area", 2.0));

    namelist.removeValue("inlet", "area");

    assertNull(namelist.getValue("inlet", "area"));
    assertNotNull(namelist.getValue("outlet", "area"));
  }

  // ── Structure listener ─────────────────────────────────────────────────────

  @Test
  void addInstanceFiresStructureListener() {
    List<AFFrNamelist> fired = new ArrayList<>();
    namelist.addNamelistListener(fired::add);

    namelist.addInstance("inlet");

    assertEquals(1, fired.size());
    assertEquals(namelist, fired.get(0));
  }

  @Test
  void removeInstanceFiresStructureListener() {
    namelist.addInstance("inlet");
    List<AFFrNamelist> fired = new ArrayList<>();
    namelist.addNamelistListener(fired::add);

    namelist.removeInstance("inlet");

    assertEquals(1, fired.size());
  }

  @Test
  void duplicateAddInstanceDoesNotFireStructureListener() {
    namelist.addInstance("inlet");
    List<AFFrNamelist> fired = new ArrayList<>();
    namelist.addNamelistListener(fired::add);

    namelist.addInstance("inlet");

    assertEquals(0, fired.size());
  }

  @Test
  void removedStructureListenerDoesNotFire() {
    List<AFFrNamelist> fired = new ArrayList<>();
    AFFrNamelistListener listener = fired::add;
    namelist.addNamelistListener(listener);
    namelist.removeNamelistListener(listener);

    namelist.addInstance("inlet");

    assertEquals(0, fired.size());
  }

  // ── Value listener fires per instance ─────────────────────────────────────

  @Test
  void valueListenerReceivesCorrectInstanceKey() {
    namelist.addInstance("inlet");
    namelist.addInstance("outlet");
    List<String> keys = new ArrayList<>();
    namelist.addValueListener("boundary_type", (key, val) -> keys.add(key));

    namelist.setValue("outlet", "boundary_type", new AFFrCharacter("boundary_type", "outlet"));

    assertEquals(1, keys.size());
    assertEquals("outlet", keys.get(0));
  }

  @Test
  void valueListenerFiresForEachInstanceOnFireAll() {
    namelist.addInstance("inlet");
    namelist.addInstance("outlet");
    namelist.setValue("inlet", "area", new AFFrReal("area", 1.0));
    namelist.setValue("outlet", "area", new AFFrReal("area", 2.0));

    List<@Nullable AFFrValue> received = new ArrayList<>();
    namelist.addValueListener("area", (key, val) -> received.add(val));

    namelist.fireAllValueListeners();

    assertEquals(2, received.size());
  }

  // ── nextInstanceKey ────────────────────────────────────────────────────────

  @Test
  void nextInstanceKeyStartsAtOne() {
    assertEquals("boundary_01", namelist.nextInstanceKey());
  }

  @Test
  void nextInstanceKeyIncrementsAfterAdd() {
    namelist.addInstance("boundary_01");
    assertEquals("boundary_02", namelist.nextInstanceKey());
  }

  @Test
  void nextInstanceKeySkipsNonNumericInstances() {
    namelist.addInstance("inlet");
    assertEquals("boundary_01", namelist.nextInstanceKey());
  }

  // ── clearInstances ─────────────────────────────────────────────────────────

  @Test
  void clearInstancesRemovesAllInstances() {
    namelist.addInstance("inlet");
    namelist.addInstance("outlet");

    namelist.clearInstances();

    assertNull(namelist.getData("inlet"));
    assertNull(namelist.getData("outlet"));
    assertTrue(namelist.getInstanceKeyList().isEmpty());
  }

  @Test
  void clearInstancesFiresStructureListenerPerInstance() {
    List<AFFrNamelist> fired = new ArrayList<>();
    namelist.addInstance("inlet");
    namelist.addInstance("outlet");
    namelist.addNamelistListener(fired::add);

    namelist.clearInstances();

    assertEquals(2, fired.size());
  }

  @Test
  void clearInstancesOnEmptyNamelistIsNoOp() {
    List<AFFrNamelist> fired = new ArrayList<>();
    namelist.addNamelistListener(fired::add);

    namelist.clearInstances();

    assertEquals(0, fired.size());
  }

  // ── Metadata ───────────────────────────────────────────────────────────────

  @Test
  void keyVariableForBoundary() {
    assertEquals("boundary_name", namelist.getKeyVariable());
  }

  @Test
  void instancePrefixForBoundary() {
    assertEquals("boundary_", namelist.getInstancePrefix());
  }

  @Test
  void customKeyVariableAndPrefix() {
    AFFrNamelistMulti custom = new AFFrNamelistMulti("CUSTOM", "my_key", "custom_");
    assertEquals("my_key", custom.getKeyVariable());
    assertEquals("custom_", custom.getInstancePrefix());
  }
}
