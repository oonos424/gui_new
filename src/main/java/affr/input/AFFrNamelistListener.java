package affr.input;

/**
 * Listener notified when the instance structure of an {@link AFFrNamelistMulti} changes.
 *
 * <p>Fires when an instance is added via {@link AFFrNamelistMulti#addInstance(String)} or removed
 * via {@link AFFrNamelistMulti#removeInstance(String)}. List-and-detail views register this
 * listener to keep their item list in sync with the model.
 *
 * @see AFFrNamelist#addNamelistListener(AFFrNamelistListener)
 */
@FunctionalInterface
public interface AFFrNamelistListener {

  /**
   * Called after the structure of {@code source} has changed (an instance was added or removed).
   *
   * @param source the namelist whose instance set changed
   */
  void onStructureChanged(AFFrNamelist source);
}
