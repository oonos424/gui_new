package affr.fx.viewmodel.top;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Lightweight guard-rail tests for {@link TopCategory}.
 *
 * <p>The label strings here are temporary stand-ins (per the class doc), so this test deliberately
 * pins them — when they move to a resource bundle, this test should be deleted or re-pointed at the
 * bundle.
 */
final class TopCategoryTest {

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
  void everyValueExposesNonEmptyLabel() {
    for (TopCategory c : TopCategory.values()) {
      String label = c.label();
      assertNotNull(label, () -> c + " has null label");
      assertEquals(false, label.isEmpty(), () -> c + " has empty label");
    }
  }

  @Test
  void labelsMatchCurrentSpec() {
    assertEquals("ファイル", TopCategory.FILE.label());
    assertEquals("実行中の計算", TopCategory.RUNNING.label());
    assertEquals("チュートリアル", TopCategory.TUTORIALS.label());
  }
}
