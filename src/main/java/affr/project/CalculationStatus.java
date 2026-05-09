package affr.project;

import affr.util.i18n.I18n;

/**
 * The lifecycle states a {@link CalculationItem} can occupy.
 *
 * <p>Each constant stores a resource-bundle key; the display label is resolved at call time via
 * {@link #label()}, which automatically reflects the currently active locale.
 *
 * <p>Persisted in {@code .affr_property} as the enum name (upper-snake-case), e.g. {@code
 * "SETTING"}, {@code "CAL_ABORTED"}.
 */
public enum CalculationStatus {
  SETTING("status.setting"),
  SETUP("status.setup"),
  QUEUING("status.queuing"),
  CALCULATING("status.calculating"),
  CALCULATED("status.calculated"),
  CAL_ABORTED("status.calAborted"),
  PRE_ABORTED("status.preAborted");

  private final String messageKey;

  CalculationStatus(String messageKey) {
    this.messageKey = messageKey;
  }

  /** Returns the resource-bundle key for this status (e.g. {@code "status.setting"}). */
  public String messageKey() {
    return messageKey;
  }

  /** Returns the localised display label for the currently active locale. */
  public String label() {
    return I18n.get(messageKey);
  }
}
