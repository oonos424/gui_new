package affr.util.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Application-level i18n accessor.
 *
 * <p>Backed by resource bundles at {@code affr/util/i18n/messages[_locale].properties}. Call {@link
 * #setLocale} once at startup (and again when the user changes language) to replace the active
 * bundle. UI components that need to refresh their labels should listen to {@link #bundleProperty}.
 *
 * <p>All methods are safe to call from the JavaFX Application Thread.
 */
public final class I18n {

  private static final ReadOnlyObjectWrapper<ResourceBundle> bundle =
      new ReadOnlyObjectWrapper<>(load(Locale.ENGLISH));

  private I18n() {}

  /** Returns the display string for {@code key} in the active locale, or {@code key} on miss. */
  public static String get(String key) {
    try {
      return bundle.get().getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /** Replaces the active bundle with one for {@code locale}. Must be called on the FX thread. */
  public static void setLocale(Locale locale) {
    bundle.set(load(locale));
  }

  /** Returns the locale of the currently active bundle. */
  public static Locale getLocale() {
    return bundle.get().getLocale();
  }

  /**
   * Observable property that fires whenever the bundle changes (i.e. the locale is switched).
   * Controllers can add a listener here to refresh their widgets.
   */
  public static ReadOnlyObjectProperty<ResourceBundle> bundleProperty() {
    return bundle.getReadOnlyProperty();
  }

  private static ResourceBundle load(Locale locale) {
    try {
      return ResourceBundle.getBundle("affr.util.i18n.messages", locale);
    } catch (MissingResourceException e) {
      return ResourceBundle.getBundle("affr.util.i18n.messages", Locale.ENGLISH);
    }
  }
}
