package affr.fx.viewmodel.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import affr.util.i18n.I18n;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Resource-bundle guard rails for {@link InputTab} labels and Input Editor header buttons.
 *
 * <p>Scope: prove that every {@link InputTab#labelKey()} resolves to a non-empty string in both
 * locales, and that no two tabs collapse onto the same English label. The literal strings
 * themselves are intentionally not pinned — those belong to the product team and are likely to
 * evolve.
 *
 * <p>{@link I18n} is global mutable state, so the {@link AfterEach} reset is required to avoid
 * leaking the locale into other tests.
 */
final class InputTabI18nTest {

  /** Header keys defined in {@code messages*.properties}. Must stay in sync with both bundles. */
  private static final List<String> HEADER_KEYS =
      List.of(
          "inputEditor.header.back",
          "inputEditor.header.home",
          "inputEditor.header.changeSettings",
          "inputEditor.header.run",
          "inputEditor.header.save",
          "inputEditor.header.editSubroutine",
          "inputEditor.header.confirmSettings",
          "inputEditor.header.menu");

  /**
   * Menu item keys defined in {@code messages*.properties}. Must stay in sync with both bundles.
   */
  private static final List<String> MENU_KEYS =
      List.of("inputEditor.menu.setting", "inputEditor.menu.about", "inputEditor.menu.language");

  @AfterEach
  void resetLocale() {
    I18n.setLocale(Locale.ENGLISH);
  }

  // ── Tab label keys resolve in both locales ────────────────────────────────

  @Test
  void everyTabHasNonEmptyEnglishLabel() {
    I18n.setLocale(Locale.ENGLISH);
    for (InputTab tab : InputTab.values()) {
      String label = I18n.get(tab.labelKey());
      assertNotNull(label, () -> tab + " has null English label");
      assertEquals(false, label.isEmpty(), () -> tab + " has empty English label");
    }
  }

  @Test
  void everyTabHasNonEmptyJapaneseLabel() {
    I18n.setLocale(Locale.JAPANESE);
    for (InputTab tab : InputTab.values()) {
      String label = I18n.get(tab.labelKey());
      assertNotNull(label, () -> tab + " has null Japanese label");
      assertEquals(false, label.isEmpty(), () -> tab + " has empty Japanese label");
    }
  }

  /**
   * Pins that no two tabs share the same English label — i.e. each tab is distinguishable in the
   * UI. This catches accidental copy-paste in the bundle without making translators' lives hard.
   */
  @Test
  void everyTabResolvesToDistinctEnglishLabel() {
    I18n.setLocale(Locale.ENGLISH);
    Set<String> labels = new HashSet<>();
    for (InputTab tab : InputTab.values()) {
      labels.add(I18n.get(tab.labelKey()));
    }
    assertEquals(InputTab.values().length, labels.size(), "two InputTab values share a label");
  }

  // ── Header button keys resolve in both locales ────────────────────────────

  @Test
  void everyHeaderButtonKeyResolvesInBothLocales() {
    for (Locale locale : List.of(Locale.ENGLISH, Locale.JAPANESE)) {
      I18n.setLocale(locale);
      for (String key : HEADER_KEYS) {
        String value = I18n.get(key);
        assertNotNull(value, () -> key + " resolved to null in " + locale);
        assertEquals(false, value.isEmpty(), () -> key + " is empty in " + locale);
      }
    }
  }

  @Test
  void everyMenuItemKeyResolvesInBothLocales() {
    for (Locale locale : List.of(Locale.ENGLISH, Locale.JAPANESE)) {
      I18n.setLocale(locale);
      for (String key : MENU_KEYS) {
        String value = I18n.get(key);
        assertNotNull(value, () -> key + " resolved to null in " + locale);
        assertEquals(false, value.isEmpty(), () -> key + " is empty in " + locale);
      }
    }
  }
}
