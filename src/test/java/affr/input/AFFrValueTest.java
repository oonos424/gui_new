package affr.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link AFFrValue} and its four concrete implementations. */
final class AFFrValueTest {

  // ── AFFrInteger ────────────────────────────────────────────────────────────

  @Test
  void integerGetName() {
    assertEquals("ncpu", new AFFrInteger("ncpu", 4).getName());
  }

  @Test
  void integerGetType() {
    assertEquals(AFFrValue.ValueType.INTEGER, new AFFrInteger("n", 1).getType());
  }

  @Test
  void integerGetIntegerValue() {
    assertEquals(42, new AFFrInteger("n", 42).getIntegerValue());
  }

  @Test
  void integerGetRealValueThrows() {
    assertThrows(UnsupportedOperationException.class, () -> new AFFrInteger("n", 1).getRealValue());
  }

  @Test
  void integerGetCharacterValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrInteger("n", 1).getCharacterValue());
  }

  @Test
  void integerGetLogicalValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrInteger("n", 1).getLogicalValue());
  }

  // ── AFFrReal ───────────────────────────────────────────────────────────────

  @Test
  void realGetName() {
    assertEquals("dt", new AFFrReal("dt", 0.001).getName());
  }

  @Test
  void realGetType() {
    assertEquals(AFFrValue.ValueType.REAL, new AFFrReal("dt", 0.001).getType());
  }

  @Test
  void realGetRealValue() {
    assertEquals(1.5, new AFFrReal("x", 1.5).getRealValue(), 1e-15);
  }

  @Test
  void realGetIntegerValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrReal("x", 1.0).getIntegerValue());
  }

  @Test
  void realGetCharacterValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrReal("x", 1.0).getCharacterValue());
  }

  @Test
  void realGetLogicalValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrReal("x", 1.0).getLogicalValue());
  }

  // ── AFFrCharacter ──────────────────────────────────────────────────────────

  @Test
  void characterGetName() {
    assertEquals("boundary_name", new AFFrCharacter("boundary_name", "inlet").getName());
  }

  @Test
  void characterGetType() {
    assertEquals(AFFrValue.ValueType.CHARACTER, new AFFrCharacter("s", "v").getType());
  }

  @Test
  void characterGetCharacterValue() {
    assertEquals("inlet", new AFFrCharacter("s", "inlet").getCharacterValue());
  }

  @Test
  void characterGetIntegerValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrCharacter("s", "x").getIntegerValue());
  }

  @Test
  void characterGetRealValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrCharacter("s", "x").getRealValue());
  }

  @Test
  void characterGetLogicalValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrCharacter("s", "x").getLogicalValue());
  }

  // ── AFFrLogical ────────────────────────────────────────────────────────────

  @Test
  void logicalGetName() {
    assertEquals("flag", new AFFrLogical("flag", true).getName());
  }

  @Test
  void logicalGetType() {
    assertEquals(AFFrValue.ValueType.LOGICAL, new AFFrLogical("b", true).getType());
  }

  @Test
  void logicalGetLogicalValueTrue() {
    assertEquals(true, new AFFrLogical("b", true).getLogicalValue());
  }

  @Test
  void logicalGetLogicalValueFalse() {
    assertEquals(false, new AFFrLogical("b", false).getLogicalValue());
  }

  @Test
  void logicalGetIntegerValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrLogical("b", true).getIntegerValue());
  }

  @Test
  void logicalGetRealValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrLogical("b", true).getRealValue());
  }

  @Test
  void logicalGetCharacterValueThrows() {
    assertThrows(
        UnsupportedOperationException.class, () -> new AFFrLogical("b", true).getCharacterValue());
  }
}
