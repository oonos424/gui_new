package affr.fx.viewmodel.top;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import affr.util.i18n.I18n;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Guard-rail tests for {@link TopCategory}.
 *
 * <p>Labels are now backed by resource bundles; tests verify the English and Japanese bundles
 * rather than pinning raw strings.
 */
final class TopCategoryTest {

  @AfterEach
  void resetLocale() {
    I18n.setLocale(Locale.ENGLISH);
  }

  @Test
  void enumDeclarationOrderIsStable() {
    // The View renders categories in declaration order via TopViewModel#getCategories,
    // so this order is part of the user-visible contract.
    TopCategory[] values = TopCategory.values();

    assertEquals(3, values.length);
    assertEquals(TopCategory.FILE, values[0]);
    assertEquals(TopCategory.RUNNING, values[1]);
    assertEquals(TopCategory.TUTORIALS, values[2]);
  }

  @Test
  void everyValueExposesNonEmptyLabelInEnglish() {
    I18n.setLocale(Locale.ENGLISH);
    for (TopCategory c : TopCategory.values()) {
      String label = c.label();
      assertNotNull(label, () -> c + " has null label");
      assertEquals(false, label.isEmpty(), () -> c + " has empty label");
    }
  }

  @Test
  void everyValueExposesNonEmptyLabelInJapanese() {
    I18n.setLocale(Locale.JAPANESE);
    for (TopCategory c : TopCategory.values()) {
      String label = c.label();
      assertNotNull(label, () -> c + " has null label");
      assertEquals(false, label.isEmpty(), () -> c + " has empty label");
    }
  }

  @Test
  void englishLabelsMatchBundle() {
    I18n.setLocale(Locale.ENGLISH);
    assertEquals("File", TopCategory.FILE.label());
    assertEquals("Running Jobs", TopCategory.RUNNING.label());
    assertEquals("Tutorials", TopCategory.TUTORIALS.label());
  }

  @Test
  void japaneseLabelsMatchBundle() {
    I18n.setLocale(Locale.JAPANESE);
    assertEquals("ファイル", TopCategory.FILE.label());
    assertEquals("実行中の計算", TopCategory.RUNNING.label());
    assertEquals("チュートリアル", TopCategory.TUTORIALS.label());
  }
}
