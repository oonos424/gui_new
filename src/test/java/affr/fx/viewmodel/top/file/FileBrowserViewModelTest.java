package affr.fx.viewmodel.top.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.data.BrowserEntry;
import affr.data.DataStore;
import affr.data.FolderEntry;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.value.ChangeListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link FileBrowserViewModel}.
 *
 * <p>The ViewModel uses only JavaFX-bean state (SimpleObjectProperty, ReadOnlyBooleanWrapper,
 * FXCollections.observableArrayList) which can be read, written, and observed without the JavaFX
 * Application Thread. No TestFX is required.
 *
 * <p>IO is never triggered here — a {@link DataStore} is constructed only to supply the root path;
 * no {@code loadChildren} call is made by any test.
 */
final class FileBrowserViewModelTest {

  // ── Convenience factory ────────────────────────────────────────────────────

  private static FileBrowserViewModel vmAt(@TempDir Path root) {
    return new FileBrowserViewModel(new DataStore(root));
  }

  // -------------------------------------------------------------------------
  // Initial state
  // -------------------------------------------------------------------------

  @Test
  void currentPathStartsAtRoot(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertEquals(root, vm.getCurrentPath());
    assertEquals(root, vm.currentPathProperty().get());
  }

  @Test
  void itemsAreEmptyInitially(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertTrue(vm.getItems().isEmpty());
  }

  @Test
  void loadingIsFalseInitially(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertFalse(vm.isLoading());
    assertFalse(vm.loadingProperty().get());
  }

  @Test
  void selectedItemIsNullInitially(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertNull(vm.getSelectedItem());
  }

  // -------------------------------------------------------------------------
  // isAtRoot / parentPath
  // -------------------------------------------------------------------------

  @Test
  void isAtRootIsTrueWhenCurrentPathEqualsRoot(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertTrue(vm.isAtRoot());
  }

  @Test
  void isAtRootIsFalseAfterNavigatingIntoSubdir(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    vm.setCurrentPath(root.resolve("sub"));

    assertFalse(vm.isAtRoot());
  }

  @Test
  void parentPathAtRootReturnsRoot(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertEquals(root, vm.parentPath());
  }

  @Test
  void parentPathFromSubdirReturnsRoot(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    vm.setCurrentPath(root.resolve("child"));

    assertEquals(root, vm.parentPath());
  }

  @Test
  void parentPathFromDeepSubdirReturnsImmediateParent(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    Path deep = root.resolve("a").resolve("b").resolve("c");
    vm.setCurrentPath(deep);

    assertEquals(root.resolve("a").resolve("b"), vm.parentPath());
  }

  @Test
  void parentPathIsClampedToRootAndNeverGoesAboveIt(@TempDir Path root) {
    // Simulate an adversarial setCurrentPath that is not under root.
    FileBrowserViewModel vm = vmAt(root);
    vm.setCurrentPath(Path.of("/tmp/outside"));

    // parentPath() must return root, not /tmp.
    assertEquals(root, vm.parentPath());
  }

  // -------------------------------------------------------------------------
  // Mutators + observable updates
  // -------------------------------------------------------------------------

  @Test
  void setCurrentPathUpdatesProperty(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    Path sub = root.resolve("sub");
    vm.setCurrentPath(sub);

    assertEquals(sub, vm.getCurrentPath());
    assertEquals(sub, vm.currentPathProperty().get());
  }

  @Test
  void setItemsReplacesListAtomically(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    BrowserEntry e1 = new FolderEntry(root.resolve("a"), "a");
    BrowserEntry e2 = new FolderEntry(root.resolve("b"), "b");

    vm.setItems(List.of(e1, e2));

    assertEquals(List.of(e1, e2), vm.getItems());
  }

  @Test
  void setItemsPreservesLiveListIdentity(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    var originalList = vm.getItems();

    vm.setItems(List.of(new FolderEntry(root.resolve("x"), "x")));

    assertSame(originalList, vm.getItems(), "getItems() must always return the same instance");
  }

  @Test
  void setLoadingUpdatesProperty(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    vm.setLoading(true);
    assertTrue(vm.isLoading());

    vm.setLoading(false);
    assertFalse(vm.isLoading());
  }

  @Test
  void setSelectedItemUpdatesProperty(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    BrowserEntry entry = new FolderEntry(root.resolve("x"), "x");

    vm.setSelectedItem(entry);

    assertSame(entry, vm.getSelectedItem());
  }

  @Test
  void selectedItemCanBeSetToNull(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    vm.setSelectedItem(new FolderEntry(root.resolve("x"), "x"));

    vm.setSelectedItem(null);

    assertNull(vm.getSelectedItem());
  }

  // -------------------------------------------------------------------------
  // Listener contracts
  // -------------------------------------------------------------------------

  @Test
  void currentPathListenerFiresOnChange(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    AtomicInteger calls = new AtomicInteger();
    vm.currentPathProperty().addListener((obs, o, n) -> calls.incrementAndGet());

    vm.setCurrentPath(root.resolve("sub"));

    assertEquals(1, calls.get());
  }

  @Test
  void loadingListenerFiresOnChange(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    AtomicInteger calls = new AtomicInteger();
    vm.loadingProperty().addListener((obs, o, n) -> calls.incrementAndGet());

    vm.setLoading(true);
    vm.setLoading(false);

    assertEquals(2, calls.get());
  }

  @Test
  void loadingListenerDoesNotFireWhenValueUnchanged(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    AtomicInteger calls = new AtomicInteger();
    vm.loadingProperty()
        .addListener((ChangeListener<Boolean>) (obs, o, n) -> calls.incrementAndGet());

    vm.setLoading(false); // already false

    assertEquals(0, calls.get());
  }

  // -------------------------------------------------------------------------
  // Property bean metadata
  // -------------------------------------------------------------------------

  @Test
  void currentPathPropertyHasBeanAndName(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertNull(vm.currentPathProperty().getBean());
    assertEquals("", vm.currentPathProperty().getName());
  }

  @Test
  void loadingPropertyHasBeanAndName(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertNull(vm.loadingProperty().getBean());
    assertEquals("", vm.loadingProperty().getName());
  }
}
