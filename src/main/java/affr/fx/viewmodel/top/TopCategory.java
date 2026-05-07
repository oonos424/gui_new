package affr.fx.viewmodel.top;

import affr.util.i18n.I18n;

/**
 * The three top-level navigation modes available in the Project Browser.
 *
 * <p>Each constant stores a resource-bundle key; the display label is resolved at call time via
 * {@link #label()}, which delegates to {@link I18n}. This means labels automatically reflect the
 * currently active locale without any additional wiring in the ViewModel.
 */
public enum TopCategory {
  FILE("category.file"),
  RUNNING("category.running"),
  TUTORIALS("category.tutorials");

  private final String messageKey;

  TopCategory(String messageKey) {
    this.messageKey = messageKey;
  }

  /** Returns the resource-bundle key for this category (e.g. {@code "category.file"}). */
  public String messageKey() {
    return messageKey;
  }

  /** Returns the localised display label for the currently active locale. */
  public String label() {
    return I18n.get(messageKey);
  }
}
