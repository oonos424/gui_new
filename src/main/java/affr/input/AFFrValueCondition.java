package affr.input;

import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * One condition-plus-value slot for a conditional namelist field.
 *
 * <p>A conditional field is a field that exists only when a <em>controlling field</em> (which may
 * live in a different namelist) satisfies a predicate. Each {@code AFFrValueCondition} represents
 * one such predicate together with the value to use when the predicate matches.
 *
 * <p>Conditions are registered in {@link AFFrNamelistData} as a list per conditional field name.
 * The list is evaluated in order; the slot from the first matching condition is used. Evaluation
 * logic is deferred to a future implementation step — this class carries only the data structure.
 *
 * <p>The {@link #slotValue()} is the only mutable part: it is set lazily when the condition becomes
 * active and a value is assigned by the user or loaded from file.
 */
public final class AFFrValueCondition {

  /** Name of the namelist (upper-case) that contains the controlling field. */
  private final String controllingListName;

  /**
   * Instance key of the controlling namelist's data instance. Use {@link
   * AFFrNamelistSingle#SINGLE_KEY} for single-instance namelists.
   */
  private final String controllingInstanceKey;

  /** Field name of the controlling field within its namelist instance. */
  private final String controllingValueName;

  /**
   * Predicate applied to the controlling field's current value. Receives {@code null} when the
   * controlling field is absent.
   */
  private final Predicate<@Nullable AFFrValue> predicate;

  /**
   * The value held in this slot, or {@code null} when the slot has not been assigned a value. When
   * this condition is the first matching condition and its slot is {@code null}, the conditional
   * field is treated as absent.
   */
  private @Nullable AFFrValue slotValue;

  /**
   * Creates a condition slot with no initial value assigned.
   *
   * @param controllingListName upper-case name of the namelist containing the controlling field
   * @param controllingInstanceKey instance key of the controlling namelist data
   * @param controllingValueName field name of the controlling field
   * @param predicate predicate evaluated against the controlling field's current value
   */
  public AFFrValueCondition(
      String controllingListName,
      String controllingInstanceKey,
      String controllingValueName,
      Predicate<@Nullable AFFrValue> predicate) {
    this.controllingListName = controllingListName;
    this.controllingInstanceKey = controllingInstanceKey;
    this.controllingValueName = controllingValueName;
    this.predicate = predicate;
    this.slotValue = null;
  }

  /** Upper-case name of the namelist that contains the controlling field (e.g. {@code "MODEL"}). */
  public String getControllingListName() {
    return controllingListName;
  }

  /**
   * Instance key of the controlling field's namelist data. {@link AFFrNamelistSingle#SINGLE_KEY}
   * for single-instance namelists.
   */
  public String getControllingInstanceKey() {
    return controllingInstanceKey;
  }

  /** Field name of the controlling field (e.g. {@code "flow"}). */
  public String getControllingValueName() {
    return controllingValueName;
  }

  /**
   * Returns the predicate applied to the controlling field's value to decide whether this slot is
   * active.
   */
  public Predicate<@Nullable AFFrValue> getPredicate() {
    return predicate;
  }

  /**
   * The value held in this slot, or {@code null} if no value has been assigned yet. A {@code null}
   * slot means the conditional field is absent even when this condition matches.
   */
  public @Nullable AFFrValue getSlotValue() {
    return slotValue;
  }

  /**
   * Assigns a value to this slot. Called when the condition becomes active and the user provides or
   * loads a value for the conditional field.
   *
   * @param value the new slot value, or {@code null} to mark the field as absent
   */
  public void setSlotValue(@Nullable AFFrValue value) {
    this.slotValue = value;
  }
}
