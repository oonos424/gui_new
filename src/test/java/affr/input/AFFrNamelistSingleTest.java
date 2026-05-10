package affr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrNamelistSingle}. */
final class AFFrNamelistSingleTest {

  private AFFrNamelistSingle namelist;

  @BeforeEach
  void setUp() {
    namelist = new AFFrNamelistSingle("MODEL");
  }

  // ── Basic read/write ───────────────────────────────────────────────────────

  @Test
  void getValueReturnsNullWhenAbsent() {
    assertNull(namelist.getValue("flow"));
  }

  @Test
  void setValueThenGetValue() {
    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));
    AFFrValue v = namelist.getValue("flow");
    assertEquals("incompressible", v.getCharacterValue());
  }

  @Test
  void setValueOverwritesPreviousValue() {
    namelist.setValue("ncpu", new AFFrInteger("ncpu", 1));
    namelist.setValue("ncpu", new AFFrInteger("ncpu", 4));
    assertEquals(4, namelist.getValue("ncpu").getIntegerValue());
  }

  @Test
  void removeValueMakesFieldAbsent() {
    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));
    namelist.removeValue("flow");
    assertNull(namelist.getValue("flow"));
  }

  @Test
  void removeAbsentValueIsNoOp() {
    namelist.removeValue("nonexistent");
    assertNull(namelist.getValue("nonexistent"));
  }

  // ── Base class API convenience ─────────────────────────────────────────────

  @Test
  void getValueViaBaseClassWithSingleKey() {
    namelist.setValue("flow", new AFFrCharacter("flow", "compressible"));
    assertEquals(
        "compressible",
        namelist.getValue(AFFrNamelistSingle.SINGLE_KEY, "flow").getCharacterValue());
  }

  @Test
  void getValueWithUnknownInstanceKeyThrows() {
    assertThrows(IllegalArgumentException.class, () -> namelist.getValue("no_such_key", "flow"));
  }

  // ── Listener: fires on setValue ────────────────────────────────────────────

  @Test
  void valueListenerFiresOnSetValue() {
    List<String> fired = new ArrayList<>();
    namelist.addValueListener("flow", (key, val) -> fired.add(key));

    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));

    assertEquals(1, fired.size());
    assertEquals(AFFrNamelistSingle.SINGLE_KEY, fired.get(0));
  }

  @Test
  void valueListenerReceivesNewValue() {
    List<@Nullable AFFrValue> received = new ArrayList<>();
    namelist.addValueListener("flow", (key, val) -> received.add(val));

    AFFrCharacter v = new AFFrCharacter("flow", "compressible");
    namelist.setValue("flow", v);

    assertEquals(1, received.size());
    assertEquals("compressible", received.get(0).getCharacterValue());
  }

  @Test
  void valueListenerFiresOnRemoveValueWithNullNewValue() {
    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));

    List<@Nullable AFFrValue> received = new ArrayList<>();
    namelist.addValueListener("flow", (key, val) -> received.add(val));

    namelist.removeValue("flow");

    assertEquals(1, received.size());
    assertNull(received.get(0));
  }

  @Test
  void valueListenerDoesNotFireForDifferentField() {
    List<String> fired = new ArrayList<>();
    namelist.addValueListener("ncpu", (key, val) -> fired.add(key));

    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));

    assertEquals(0, fired.size());
  }

  @Test
  void multipleListenersOnSameFieldAllFire() {
    List<Integer> counts = new ArrayList<>();
    namelist.addValueListener("flow", (key, val) -> counts.add(1));
    namelist.addValueListener("flow", (key, val) -> counts.add(2));

    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));

    assertEquals(2, counts.size());
  }

  @Test
  void removedListenerDoesNotFire() {
    List<String> fired = new ArrayList<>();
    AFFrValueListener listener = (key, val) -> fired.add(key);
    namelist.addValueListener("flow", listener);
    namelist.removeValueListener("flow", listener);

    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));

    assertEquals(0, fired.size());
  }

  // ── Listener: fireAllValueListeners ───────────────────────────────────────

  @Test
  void fireAllValueListenersPushesCurrentState() {
    namelist.setValue("flow", new AFFrCharacter("flow", "compressible"));

    List<@Nullable AFFrValue> received = new ArrayList<>();
    namelist.addValueListener("flow", (key, val) -> received.add(val));

    namelist.fireAllValueListeners();

    assertEquals(1, received.size());
    assertEquals("compressible", received.get(0).getCharacterValue());
  }

  // ── clearAllValues ─────────────────────────────────────────────────────────

  @Test
  void clearAllValuesRemovesFields() {
    namelist.setValue("flow", new AFFrCharacter("flow", "incompressible"));
    namelist.clearAllValues();
    assertNull(namelist.getValue("flow"));
  }
}
