package affr.app;

import affr.util.i18n.I18n;
import java.util.Locale;
import javafx.scene.control.RadioMenuItem;

/**
 * Wires a pair of {@link RadioMenuItem}s as a Language submenu that drives, and reflects, {@link
 * I18n#getLocale()}.
 *
 * <p>Centralises the three behaviours that every Language submenu in the app must share so they
 * cannot drift between screens:
 *
 * <ul>
 *   <li>firing a radio calls {@link I18n#setLocale(Locale)};
 *   <li>the radio matching the active locale is selected at install time;
 *   <li>external locale changes (anywhere via {@link I18n#bundleProperty()}) keep the radios in
 *       sync.
 * </ul>
 *
 * <p>Each screen still owns its own FXML-declared {@link RadioMenuItem}s (and their {@code
 * ToggleGroup}, label text, styling). This class only owns the wiring back to {@link I18n}.
 *
 * <p>Locale matching uses {@link Locale#getLanguage()} so any Japanese variant ({@code ja}, {@code
 * ja_JP}) selects the Japanese radio; everything else selects English.
 */
public final class LanguageMenu {

  private LanguageMenu() {}

  /**
   * Wires {@code english} and {@code japanese} to drive and reflect {@link I18n#getLocale()}.
   *
   * <p>Adds a listener on {@link I18n#bundleProperty()} that lives for as long as the bundle
   * property exists; intended for top-level controllers whose lifetime matches the application.
   * Must be called on the JavaFX Application Thread.
   */
  public static void install(RadioMenuItem english, RadioMenuItem japanese) {
    english.setOnAction(e -> I18n.setLocale(Locale.ENGLISH));
    japanese.setOnAction(e -> I18n.setLocale(Locale.JAPANESE));
    syncSelection(english, japanese, I18n.getLocale());
    I18n.bundleProperty()
        .addListener((obs, old, bundle) -> syncSelection(english, japanese, I18n.getLocale()));
  }

  private static void syncSelection(RadioMenuItem english, RadioMenuItem japanese, Locale locale) {
    boolean isJa = Locale.JAPANESE.getLanguage().equals(locale.getLanguage());
    japanese.setSelected(isJa);
    english.setSelected(!isJa);
  }
}
