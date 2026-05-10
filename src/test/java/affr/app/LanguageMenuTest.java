package affr.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.util.i18n.I18n;
import java.util.Locale;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Unit tests for {@link LanguageMenu}.
 *
 * <p>Pin the three contract points the helper exists to enforce:
 *
 * <ul>
 *   <li>{@link LanguageMenu#install(RadioMenuItem, RadioMenuItem)} selects the radio matching the
 *       current locale at install time;
 *   <li>firing a radio sets {@link I18n#getLocale()};
 *   <li>external locale changes via {@link I18n#bundleProperty()} keep the radios in sync.
 * </ul>
 *
 * <p>{@link I18n} is global mutable state; the {@link AfterEach} reset prevents leakage into other
 * tests. Runs headlessly on Monocle — see {@code build.gradle.kts} {@code tasks.test}.
 */
@ExtendWith(ApplicationExtension.class)
final class LanguageMenuTest {

  private RadioMenuItem english;
  private RadioMenuItem japanese;

  @Start
  void start(Stage stage) {
    // No scene needed — RadioMenuItem doesn't require attachment to a scene to fire actions.
  }

  @BeforeEach
  void setUp(FxRobot robot) {
    robot.interact(
        () -> {
          I18n.setLocale(Locale.ENGLISH);
          ToggleGroup group = new ToggleGroup();
          english = new RadioMenuItem("English");
          japanese = new RadioMenuItem("日本語");
          english.setToggleGroup(group);
          japanese.setToggleGroup(group);
        });
  }

  @AfterEach
  void tearDown(FxRobot robot) {
    robot.interact(() -> I18n.setLocale(Locale.ENGLISH));
  }

  @Test
  void installSelectsRadioMatchingCurrentLocale_english(FxRobot robot) {
    robot.interact(() -> LanguageMenu.install(english, japanese));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(english.isSelected(), "English radio must be selected for English locale");
    assertFalse(japanese.isSelected());
  }

  @Test
  void installSelectsRadioMatchingCurrentLocale_japanese(FxRobot robot) {
    robot.interact(
        () -> {
          I18n.setLocale(Locale.JAPANESE);
          LanguageMenu.install(english, japanese);
        });
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(japanese.isSelected(), "Japanese radio must be selected for Japanese locale");
    assertFalse(english.isSelected());
  }

  @Test
  void firingJapaneseRadioSwitchesLocaleAndUpdatesSelection(FxRobot robot) {
    robot.interact(() -> LanguageMenu.install(english, japanese));

    robot.interact(() -> japanese.fire());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(Locale.JAPANESE, I18n.getLocale());
    assertTrue(japanese.isSelected());
    assertFalse(english.isSelected());
  }

  /**
   * Firing the English radio while the locale is Japanese must switch the locale away from Japanese
   * and re-sync the selection. We don't assert {@code I18n.getLocale() == ENGLISH} because, with no
   * {@code messages_en.properties} on the classpath, the underlying {@link
   * java.util.ResourceBundle} resolves to the base bundle whose {@link Locale} is {@link
   * Locale#ROOT} (empty language). What {@link LanguageMenu} actually owns is the radio selection,
   * which we pin directly.
   */
  @Test
  void firingEnglishRadioSwitchesLocaleAwayFromJapanese(FxRobot robot) {
    robot.interact(
        () -> {
          I18n.setLocale(Locale.JAPANESE);
          LanguageMenu.install(english, japanese);
        });

    robot.interact(() -> english.fire());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(
        false,
        Locale.JAPANESE.getLanguage().equals(I18n.getLocale().getLanguage()),
        "firing English radio must drop the Japanese locale");
    assertTrue(english.isSelected());
    assertFalse(japanese.isSelected());
  }

  /**
   * External locale changes — i.e. driven by another component, not by firing one of these radios —
   * must still be reflected in this menu.
   */
  @Test
  void externalLocaleChangeKeepsRadiosInSync(FxRobot robot) {
    robot.interact(() -> LanguageMenu.install(english, japanese));

    robot.interact(() -> I18n.setLocale(Locale.JAPANESE));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(japanese.isSelected(), "Japanese radio must follow external locale change");
    assertFalse(english.isSelected());

    robot.interact(() -> I18n.setLocale(Locale.ENGLISH));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(english.isSelected(), "English radio must follow external locale change back");
    assertFalse(japanese.isSelected());
  }

  /**
   * Any Japanese variant ({@code ja}, {@code ja_JP}, ...) must select the Japanese radio. Pins the
   * documented {@link Locale#getLanguage()} matching contract.
   */
  @Test
  void japaneseRegionalVariantStillSelectsJapaneseRadio(FxRobot robot) {
    robot.interact(() -> LanguageMenu.install(english, japanese));

    robot.interact(() -> I18n.setLocale(Locale.JAPAN));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(japanese.isSelected(), "ja_JP must select the Japanese radio");
    assertFalse(english.isSelected());
  }
}
