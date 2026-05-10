package affr.input;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Listener notified when a field value in an {@link AFFrNamelist} changes.
 *
 * <p>Registered via {@link AFFrNamelist#addValueListener(String, AFFrValueListener)} for a specific
 * field name. The listener fires after every {@link AFFrNamelist#setValue} or {@link
 * AFFrNamelist#removeValue} call on that field, regardless of which namelist instance was modified.
 *
 * <p>Binding components that are locked to a specific namelist instance should filter by {@code
 * instanceKey} inside their implementation if they only care about one instance.
 *
 * @see AFFrNamelist#addValueListener(String, AFFrValueListener)
 */
@FunctionalInterface
public interface AFFrValueListener {

  /**
   * Called after a field value has changed.
   *
   * @param instanceKey the key of the namelist instance whose field changed (always {@code
   *     "single"} for single-instance namelists)
   * @param newValue the new value of the field, or {@code null} if the field was removed
   */
  void onValueChanged(String instanceKey, @Nullable AFFrValue newValue);
}
