package affr.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract base for a solver namelist, holding one or more {@link AFFrNamelistData} instances and
 * the observer infrastructure for value-change and structure-change notifications.
 *
 * <p>Two concrete subclasses cover the two kinds of namelists in {@code fflow.ctl}:
 *
 * <ul>
 *   <li>{@link AFFrNamelistSingle} — exactly one data instance (keyed {@link
 *       AFFrNamelistSingle#SINGLE_KEY}).
 *   <li>{@link AFFrNamelistMulti} — one data instance per domain entity (boundary, region, probe,
 *       etc.).
 * </ul>
 *
 * <p>All writes go through {@link #setValue} and {@link #removeValue}, which are the single choke
 * point that fires {@link AFFrValueListener}s. Direct mutation of {@link AFFrNamelistData} from
 * outside this class is not possible — the data write methods are package-private.
 */
public abstract sealed class AFFrNamelist permits AFFrNamelistSingle, AFFrNamelistMulti {

  /** Upper-case namelist name as it appears in {@code fflow.ctl} (e.g. {@code "BOUNDARY"}). */
  protected final String listName;

  /**
   * Instance key → data map. Package-visible so subclasses can seed it during construction (e.g.
   * {@link AFFrNamelistSingle} inserts its single instance, {@link AFFrNamelistMulti} adds
   * instances dynamically).
   */
  protected final Map<String, AFFrNamelistData> nmlistData = new HashMap<>();

  /**
   * Listeners per field name. Each listener fires when <em>any</em> instance's value for that field
   * changes.
   */
  private final Map<String, List<AFFrValueListener>> valueListeners = new HashMap<>();

  /** Listeners notified when the set of instances changes (add / remove). */
  private final List<AFFrNamelistListener> namelistListeners = new ArrayList<>();

  /** Creates a namelist with the given upper-case name. */
  protected AFFrNamelist(String listName) {
    this.listName = listName;
  }

  // ── Identity ──────────────────────────────────────────────────────────────

  /** Upper-case namelist name (e.g. {@code "BOUNDARY"}). */
  public String getListName() {
    return listName;
  }

  // ── Read API ──────────────────────────────────────────────────────────────

  /**
   * Returns the value of a field in the named instance, or {@code null} if the field is absent.
   *
   * @param instanceKey the instance key (always {@link AFFrNamelistSingle#SINGLE_KEY} for
   *     single-instance namelists)
   * @param valueName field name (lower-case)
   * @return the current value, or {@code null}
   * @throws IllegalArgumentException if {@code instanceKey} does not exist in this namelist
   */
  public @Nullable AFFrValue getValue(String instanceKey, String valueName) {
    AFFrNamelistData data = requireData(instanceKey);
    return data.getValue(valueName);
  }

  /**
   * Returns the {@link AFFrNamelistData} for the named instance, or {@code null} if no such
   * instance exists.
   */
  public @Nullable AFFrNamelistData getData(String instanceKey) {
    return nmlistData.get(instanceKey);
  }

  /** Returns an unmodifiable view of the current instance keys. */
  public Set<String> getInstanceKeys() {
    return Collections.unmodifiableSet(nmlistData.keySet());
  }

  // ── Write API ─────────────────────────────────────────────────────────────

  /**
   * Sets the value of a field in the named instance and notifies all registered listeners for that
   * field name.
   *
   * @param instanceKey the instance key
   * @param valueName field name (lower-case)
   * @param value the new value
   * @throws IllegalArgumentException if {@code instanceKey} does not exist in this namelist
   */
  public void setValue(String instanceKey, String valueName, AFFrValue value) {
    requireData(instanceKey).putValue(valueName, value);
    fireValueListeners(instanceKey, valueName, value);
  }

  /**
   * Removes the value of a field in the named instance (marks the field as absent) and notifies all
   * registered listeners for that field name with a {@code null} new-value.
   *
   * @param instanceKey the instance key
   * @param valueName field name (lower-case)
   * @throws IllegalArgumentException if {@code instanceKey} does not exist in this namelist
   */
  public void removeValue(String instanceKey, String valueName) {
    requireData(instanceKey).removeValue(valueName);
    fireValueListeners(instanceKey, valueName, null);
  }

  /**
   * Removes all field values from every instance. Conditional field registrations and condition
   * structures are preserved. Called during model reload before re-parsing the file.
   *
   * <p>Does not fire individual value listeners — the caller is expected to fire all notifications
   * in bulk after reloading by calling {@link #fireAllValueListeners()}.
   */
  public void clearAllValues() {
    for (AFFrNamelistData data : nmlistData.values()) {
      data.clearValues();
    }
  }

  // ── Conditional field registration ────────────────────────────────────────

  /**
   * Registers a conditional field in the named instance and appends the given condition slot.
   *
   * <p>The field is registered (if not already) and the condition is appended to its condition
   * list. Conditions are evaluated in registration order; the first matching slot wins.
   *
   * @param instanceKey the instance key
   * @param valueName field name of the conditional field (lower-case)
   * @param condition the condition slot to append
   * @throws IllegalArgumentException if {@code instanceKey} does not exist in this namelist
   */
  public void registerCondition(
      String instanceKey, String valueName, AFFrValueCondition condition) {
    AFFrNamelistData data = requireData(instanceKey);
    data.registerConditionalField(valueName);
    data.addCondition(valueName, condition);
  }

  // ── Listener registration ─────────────────────────────────────────────────

  /**
   * Registers a listener that fires whenever the named field changes in any instance of this
   * namelist.
   *
   * <p>The same listener instance can be registered multiple times for different field names. There
   * is no deduplication — adding a listener twice for the same field name causes it to fire twice
   * per change.
   *
   * @param valueName field name to observe (lower-case)
   * @param listener the listener to register
   */
  public void addValueListener(String valueName, AFFrValueListener listener) {
    valueListeners.computeIfAbsent(valueName, k -> new ArrayList<>()).add(listener);
  }

  /**
   * Removes a previously registered value listener for the named field.
   *
   * <p>If the listener was not registered for {@code valueName}, this is a no-op.
   *
   * @param valueName field name (lower-case)
   * @param listener the listener to remove
   */
  public void removeValueListener(String valueName, AFFrValueListener listener) {
    List<AFFrValueListener> list = valueListeners.get(valueName);
    if (list != null) {
      list.remove(listener);
    }
  }

  /**
   * Registers a listener that fires whenever the instance set of this namelist changes (an instance
   * is added or removed).
   *
   * @param listener the listener to register
   */
  public void addNamelistListener(AFFrNamelistListener listener) {
    namelistListeners.add(listener);
  }

  /**
   * Removes a previously registered structure listener.
   *
   * @param listener the listener to remove
   */
  public void removeNamelistListener(AFFrNamelistListener listener) {
    namelistListeners.remove(listener);
  }

  // ── Bulk notification ─────────────────────────────────────────────────────

  /**
   * Fires all registered value listeners for every field in every instance. Used after a bulk
   * reload so that every bound widget refreshes from the new state.
   */
  public void fireAllValueListeners() {
    for (Map.Entry<String, AFFrNamelistData> entry : nmlistData.entrySet()) {
      String instanceKey = entry.getKey();
      AFFrNamelistData data = entry.getValue();
      for (String valueName : valueListeners.keySet()) {
        @Nullable AFFrValue current = data.getValue(valueName);
        fireValueListeners(instanceKey, valueName, current);
      }
    }
  }

  // ── Internal helpers ──────────────────────────────────────────────────────

  private AFFrNamelistData requireData(String instanceKey) {
    AFFrNamelistData data = nmlistData.get(instanceKey);
    if (data == null) {
      throw new IllegalArgumentException(
          "No instance '" + instanceKey + "' in namelist " + listName);
    }
    return data;
  }

  private void fireValueListeners(
      String instanceKey, String valueName, @Nullable AFFrValue newValue) {
    List<AFFrValueListener> list = valueListeners.get(valueName);
    if (list == null || list.isEmpty()) return;
    for (int i = 0, n = list.size(); i < n; i++) {
      list.get(i).onValueChanged(instanceKey, newValue);
    }
  }

  /**
   * Fires all registered structure listeners. Called by subclasses ({@link AFFrNamelistMulti})
   * after an instance is added or removed.
   */
  protected void fireNamelistListeners() {
    for (int i = 0, n = namelistListeners.size(); i < n; i++) {
      namelistListeners.get(i).onStructureChanged(this);
    }
  }
}
