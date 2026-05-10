package affr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrNamelistData}. */
final class AFFrNamelistDataTest {

  private AFFrNamelistData data;

  @BeforeEach
  void setUp() {
    data = new AFFrNamelistData("BOUNDARY", "inlet");
  }

  // ── Identity ───────────────────────────────────────────────────────────────

  @Test
  void getInstanceKey() {
    assertEquals("inlet", data.getInstanceKey());
  }

  @Test
  void getListName() {
    assertEquals("BOUNDARY", data.getListName());
  }

  // ── Regular field read/write ───────────────────────────────────────────────

  @Test
  void getValueReturnsNullWhenAbsent() {
    assertNull(data.getValue("boundary_type"));
  }

  @Test
  void putValueThenGetValue() {
    data.putValue("boundary_type", new AFFrCharacter("boundary_type", "wall"));
    assertEquals("wall", data.getValue("boundary_type").getCharacterValue());
  }

  @Test
  void putValueOverwritesExistingValue() {
    data.putValue("area", new AFFrReal("area", 1.0));
    data.putValue("area", new AFFrReal("area", 2.5));
    assertEquals(2.5, data.getValue("area").getRealValue(), 1e-15);
  }

  @Test
  void removeValueMakesFieldAbsent() {
    data.putValue("area", new AFFrReal("area", 1.0));
    data.removeValue("area");
    assertNull(data.getValue("area"));
  }

  @Test
  void removeAbsentValueIsNoOp() {
    data.removeValue("nonexistent");
    assertNull(data.getValue("nonexistent"));
  }

  @Test
  void getValueNamesReflectsPresentFields() {
    data.putValue("a", new AFFrInteger("a", 1));
    data.putValue("b", new AFFrInteger("b", 2));
    assertTrue(data.getValueNames().contains("a"));
    assertTrue(data.getValueNames().contains("b"));
  }

  // ── Conditional field registration ────────────────────────────────────────

  @Test
  void isConditionalReturnsFalseByDefault() {
    assertFalse(data.isConditional("some_field"));
  }

  @Test
  void registerConditionalFieldMakesIsConditionalTrue() {
    data.registerConditionalField("sub_type");
    assertTrue(data.isConditional("sub_type"));
  }

  @Test
  void registerConditionalFieldCreatesEmptyConditionList() {
    data.registerConditionalField("sub_type");
    assertTrue(data.getConditions("sub_type").isEmpty());
  }

  @Test
  void getConditionsReturnsEmptyListForUnregisteredField() {
    assertTrue(data.getConditions("unknown").isEmpty());
  }

  @Test
  void addConditionAppendsToList() {
    data.registerConditionalField("sub_type");
    AFFrValueCondition c = new AFFrValueCondition("MODEL", "single", "flow", v -> true);
    data.addCondition("sub_type", c);
    assertEquals(1, data.getConditions("sub_type").size());
    assertEquals(c, data.getConditions("sub_type").get(0));
  }

  @Test
  void addConditionPreservesOrder() {
    data.registerConditionalField("sub_type");
    AFFrValueCondition c1 = new AFFrValueCondition("MODEL", "single", "flow", v -> true);
    AFFrValueCondition c2 = new AFFrValueCondition("MODEL", "single", "flow", v -> false);
    data.addCondition("sub_type", c1);
    data.addCondition("sub_type", c2);

    List<AFFrValueCondition> list = data.getConditions("sub_type");
    assertEquals(c1, list.get(0));
    assertEquals(c2, list.get(1));
  }

  @Test
  void addConditionToUnregisteredFieldThrows() {
    AFFrValueCondition c = new AFFrValueCondition("MODEL", "single", "flow", v -> true);
    assertThrows(IllegalStateException.class, () -> data.addCondition("sub_type", c));
  }

  @Test
  void getConditionalFieldNamesReflectsRegistrations() {
    data.registerConditionalField("sub_type");
    data.registerConditionalField("sub_value");
    assertTrue(data.getConditionalFieldNames().contains("sub_type"));
    assertTrue(data.getConditionalFieldNames().contains("sub_value"));
  }

  // ── clearValues ────────────────────────────────────────────────────────────

  @Test
  void clearValuesRemovesRegularFields() {
    data.putValue("area", new AFFrReal("area", 1.0));
    data.clearValues();
    assertNull(data.getValue("area"));
  }

  @Test
  void clearValuesPreservesConditionalFieldRegistration() {
    data.registerConditionalField("sub_type");
    data.clearValues();
    assertTrue(data.isConditional("sub_type"));
  }

  @Test
  void clearValuesNullsConditionalSlotValues() {
    data.registerConditionalField("sub_type");
    AFFrValueCondition c = new AFFrValueCondition("MODEL", "single", "flow", v -> true);
    c.setSlotValue(new AFFrInteger("sub_type", 1));
    data.addCondition("sub_type", c);

    data.clearValues();

    assertNull(data.getConditions("sub_type").get(0).getSlotValue());
  }
}
