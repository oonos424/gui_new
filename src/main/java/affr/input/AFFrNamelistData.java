package affr.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * One instance of a namelist block, holding all field values for that instance.
 *
 * <p>For single-instance namelists there is always exactly one {@code AFFrNamelistData} keyed
 * {@link AFFrNamelistSingle#SINGLE_KEY}. For multi-instance namelists there is one per domain
 * instance (boundary, region, probe, etc.).
 *
 * <p>Fields are divided into two categories:
 *
 * <ul>
 *   <li><b>Regular fields</b> — always present regardless of other field values.
 *   <li><b>Conditional fields</b> — present only when a controlling field satisfies a condition.
 *       The condition list is registered via {@link #registerConditionalField} and the slot values
 *       are populated later; evaluation is deferred.
 * </ul>
 *
 * <p>Write access is package-private. External code must write through {@link AFFrNamelist}, which
 * is the single choke point that fires value-change listeners.
 */
public final class AFFrNamelistData {

  private final String instanceKey;
  private final String listName;

  private final Map<String, AFFrValue> regularValues = new HashMap<>();
  private final Map<String, List<AFFrValueCondition>> conditionalFields = new HashMap<>();

  /**
   * Creates an empty namelist data instance.
   *
   * @param listName upper-case namelist name (e.g. {@code "BOUNDARY"})
   * @param instanceKey the key that identifies this instance within its parent namelist
   */
  AFFrNamelistData(String listName, String instanceKey) {
    this.listName = listName;
    this.instanceKey = instanceKey;
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  /** The key that identifies this instance within its parent {@link AFFrNamelist}. */
  public String getInstanceKey() {
    return instanceKey;
  }

  /** Upper-case name of the namelist this data belongs to (e.g. {@code "BOUNDARY"}). */
  public String getListName() {
    return listName;
  }

  /**
   * Returns the current value of the named regular field, or {@code null} if the field is absent.
   *
   * <p>Conditional fields are not returned by this method; their values live in {@link
   * AFFrValueCondition#getSlotValue()}.
   *
   * @param valueName field name (lower-case)
   * @return the value, or {@code null}
   */
  public @Nullable AFFrValue getValue(String valueName) {
    return regularValues.get(valueName);
  }

  /** Returns an unmodifiable view of all regular field names currently present in this instance. */
  public Set<String> getValueNames() {
    return Collections.unmodifiableSet(regularValues.keySet());
  }

  /**
   * Returns an unmodifiable view of the condition list for the named conditional field, or an empty
   * list if the field has not been registered as conditional.
   *
   * @param valueName field name of the conditional field (lower-case)
   */
  public List<AFFrValueCondition> getConditions(String valueName) {
    List<AFFrValueCondition> conditions = conditionalFields.get(valueName);
    return conditions != null ? Collections.unmodifiableList(conditions) : List.of();
  }

  /**
   * Returns {@code true} if the named field is registered as a conditional field (regardless of
   * whether any condition currently matches).
   */
  public boolean isConditional(String valueName) {
    return conditionalFields.containsKey(valueName);
  }

  /** Returns an unmodifiable view of all conditional field names registered in this instance. */
  public Set<String> getConditionalFieldNames() {
    return Collections.unmodifiableSet(conditionalFields.keySet());
  }

  // ── Package-private write methods (called only by AFFrNamelist) ───────────

  /**
   * Stores a value for a regular field. Replaces any existing value for the same name.
   *
   * @param valueName field name (lower-case)
   * @param value the new value
   */
  void putValue(String valueName, AFFrValue value) {
    regularValues.put(valueName, value);
  }

  /**
   * Removes a regular field value. A no-op if the field is absent.
   *
   * @param valueName field name (lower-case)
   */
  void removeValue(String valueName) {
    regularValues.remove(valueName);
  }

  /**
   * Registers a conditional field with an initial (empty) condition list.
   *
   * <p>Call {@link #addCondition} to add individual condition slots after registration.
   *
   * @param valueName field name of the conditional field (lower-case)
   */
  void registerConditionalField(String valueName) {
    conditionalFields.putIfAbsent(valueName, new ArrayList<>());
  }

  /**
   * Appends a condition slot to the end of the condition list for the named conditional field.
   *
   * <p>{@link #registerConditionalField} must be called first.
   *
   * @param valueName field name of the conditional field (lower-case)
   * @param condition the condition slot to append
   * @throws IllegalStateException if the field has not been registered as conditional
   */
  void addCondition(String valueName, AFFrValueCondition condition) {
    List<AFFrValueCondition> conditions = conditionalFields.get(valueName);
    if (conditions == null) {
      throw new IllegalStateException(
          "Field '" + valueName + "' has not been registered as conditional in " + listName);
    }
    conditions.add(condition);
  }

  /**
   * Removes all regular field values. Conditional field registrations and their condition lists are
   * preserved (the lists are cleared of slot values, but the structure remains). Called during
   * model reload.
   */
  void clearValues() {
    regularValues.clear();
    for (List<AFFrValueCondition> conditions : conditionalFields.values()) {
      for (AFFrValueCondition c : conditions) {
        c.setSlotValue(null);
      }
    }
  }
}
