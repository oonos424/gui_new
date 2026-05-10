package affr.input;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A single-instance namelist — one that appears exactly once in {@code fflow.ctl}.
 *
 * <p>Examples: {@code &MODEL}, {@code &TIME}, {@code &GRAVITY}, {@code &FLAGS}.
 *
 * <p>This class wraps the base {@link AFFrNamelist} API with convenience overloads that omit the
 * instance key, always targeting the single internal instance keyed {@link #SINGLE_KEY}.
 */
public final class AFFrNamelistSingle extends AFFrNamelist {

  /**
   * The fixed instance key used for the sole data instance inside every single-instance namelist.
   */
  public static final String SINGLE_KEY = "single";

  /**
   * Creates a single-instance namelist and initialises the sole data instance.
   *
   * @param listName upper-case namelist name (e.g. {@code "MODEL"})
   */
  public AFFrNamelistSingle(String listName) {
    super(listName);
    nmlistData.put(SINGLE_KEY, new AFFrNamelistData(listName, SINGLE_KEY));
  }

  // ── Convenience single-instance overloads ─────────────────────────────────

  /**
   * Sets the value of a field in this namelist and notifies all registered listeners.
   *
   * <p>Equivalent to {@code setValue(SINGLE_KEY, valueName, value)}.
   *
   * @param valueName field name (lower-case)
   * @param value the new value
   */
  public void setValue(String valueName, AFFrValue value) {
    setValue(SINGLE_KEY, valueName, value);
  }

  /**
   * Returns the current value of a field, or {@code null} if the field is absent.
   *
   * <p>Equivalent to {@code getValue(SINGLE_KEY, valueName)}.
   *
   * @param valueName field name (lower-case)
   * @return the current value, or {@code null}
   */
  public @Nullable AFFrValue getValue(String valueName) {
    return getValue(SINGLE_KEY, valueName);
  }

  /**
   * Removes a field from this namelist and notifies all registered listeners.
   *
   * <p>Equivalent to {@code removeValue(SINGLE_KEY, valueName)}.
   *
   * @param valueName field name (lower-case)
   */
  public void removeValue(String valueName) {
    removeValue(SINGLE_KEY, valueName);
  }

  /**
   * Registers a conditional field condition for this namelist's single instance.
   *
   * <p>Equivalent to {@code registerCondition(SINGLE_KEY, valueName, condition)}.
   *
   * @param valueName field name of the conditional field (lower-case)
   * @param condition the condition slot to append
   */
  public void registerCondition(String valueName, AFFrValueCondition condition) {
    registerCondition(SINGLE_KEY, valueName, condition);
  }

  /** Returns the sole {@link AFFrNamelistData} instance for this namelist. Never {@code null}. */
  public AFFrNamelistData getData() {
    AFFrNamelistData data = nmlistData.get(SINGLE_KEY);
    if (data == null) {
      throw new IllegalStateException("Single-instance data missing for namelist " + listName);
    }
    return data;
  }
}
