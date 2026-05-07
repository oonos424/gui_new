package affr.fx.viewmodel.top;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TopViewModel}.
 *
 * <p>The ViewModel exposes only JavaFX-bean state — {@link
 * javafx.beans.property.SimpleObjectProperty} and {@link
 * javafx.collections.FXCollections#observableArrayList} both work without the JavaFX Application
 * Thread for property reads, writes, and listener notifications, so these tests do not need TestFX.
 */
final class TopViewModelTest {

  @Test
  void initialCategoriesReflectEnumDeclarationOrder() {
    TopViewModel vm = new TopViewModel();

    ObservableList<TopCategory> categories = vm.getCategories();

    assertEquals(List.of(TopCategory.FILE, TopCategory.RUNNING, TopCategory.TUTORIALS), categories);
  }

  @Test
  void defaultSelectedCategoryIsFile() {
    TopViewModel vm = new TopViewModel();

    assertSame(TopCategory.FILE, vm.getSelectedCategory());
    assertSame(TopCategory.FILE, vm.selectedCategoryProperty().get());
  }

  @Test
  void setSelectedCategoryUpdatesProperty() {
    TopViewModel vm = new TopViewModel();

    vm.setSelectedCategory(TopCategory.TUTORIALS);

    assertSame(TopCategory.TUTORIALS, vm.getSelectedCategory());
    assertSame(TopCategory.TUTORIALS, vm.selectedCategoryProperty().get());
  }

  @Test
  void listenersFireOnChange() {
    TopViewModel vm = new TopViewModel();
    AtomicInteger calls = new AtomicInteger();
    ChangeListener<TopCategory> listener = (obs, oldV, newV) -> calls.incrementAndGet();
    vm.selectedCategoryProperty().addListener(listener);

    vm.setSelectedCategory(TopCategory.RUNNING);
    vm.setSelectedCategory(TopCategory.TUTORIALS);

    assertEquals(2, calls.get());
  }

  @Test
  void listenerDoesNotFireWhenSettingSameValue() {
    // This is the property the View relies on to avoid re-entrancy when it writes
    // the VM's current selection back into the ListView selection model.
    TopViewModel vm = new TopViewModel();
    AtomicInteger calls = new AtomicInteger();
    vm.selectedCategoryProperty().addListener((obs, oldV, newV) -> calls.incrementAndGet());

    vm.setSelectedCategory(vm.getSelectedCategory());

    assertEquals(0, calls.get());
  }

  @Test
  void categoriesListIsLiveBackingCollection() {
    // The View binds ListView#items directly to this list, so getCategories must return
    // the same instance every time (otherwise replacing the items breaks bindings).
    TopViewModel vm = new TopViewModel();

    ObservableList<TopCategory> first = vm.getCategories();
    ObservableList<TopCategory> second = vm.getCategories();

    assertSame(first, second);
  }

  @Test
  void selectedCategoryPropertyHasStableIdentity() {
    // Same contract as above for the selectedCategory property.
    TopViewModel vm = new TopViewModel();

    assertSame(vm.selectedCategoryProperty(), vm.selectedCategoryProperty());
  }

  @Test
  void selectedCategoryPropertyExposesBeanAndName() {
    // Bean metadata is part of the JavaFX-bean contract; useful for debug tooling and
    // anything that introspects properties (e.g. Scene Builder, reflective binders).
    TopViewModel vm = new TopViewModel();

    assertSame(vm, vm.selectedCategoryProperty().getBean());
    assertEquals("selectedCategory", vm.selectedCategoryProperty().getName());
  }

  @Test
  void changeListenerSeesOldAndNewValues() {
    TopViewModel vm = new TopViewModel();
    final TopCategory[] seenOld = {null};
    final TopCategory[] seenNew = {null};
    vm.selectedCategoryProperty()
        .addListener(
            (obs, oldV, newV) -> {
              seenOld[0] = oldV;
              seenNew[0] = newV;
            });

    vm.setSelectedCategory(TopCategory.RUNNING);

    assertSame(TopCategory.FILE, seenOld[0]);
    assertSame(TopCategory.RUNNING, seenNew[0]);
  }

  @Test
  void allEnumValuesAreReachableThroughSelection() {
    TopViewModel vm = new TopViewModel();

    for (TopCategory c : TopCategory.values()) {
      vm.setSelectedCategory(c);
      assertSame(c, vm.getSelectedCategory());
    }

    assertTrue(vm.getCategories().containsAll(List.of(TopCategory.values())));
  }
}
