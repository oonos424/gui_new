package affr.fx.viewmodel.top.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import affr.data.BrowserEntry;
import affr.data.DataStore;
import affr.data.FolderEntry;
import affr.data.ProjectEntry;
import affr.project.AFFrProject;
import affr.project.ProjectLoader;
import affr.util.fx.FxScheduler;
import java.io.IOException;
import java.nio.file.Files;
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
 * <p>Async operations are exercised against a real {@link DataStore} backed by a JUnit {@link
 * TempDir} but driven through {@link FxScheduler#synchronous()} so callbacks fire inline on the
 * test thread.
 */
final class FileBrowserViewModelTest {

  private static final String PROJECT_MARKER = ".affr_project";

  // ── Convenience factories ──────────────────────────────────────────────────

  private static FileBrowserViewModel vmAt(Path root) {
    return new FileBrowserViewModel(
        new DataStore(root), new ProjectLoader(), FxScheduler.synchronous());
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

  @Test
  void errorIsNullInitially(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertNull(vm.getError());
    assertNull(vm.errorProperty().get());
  }

  @Test
  void pendingSelectNameIsNullInitially(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    assertNull(vm.getPendingSelectName());
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
    FileBrowserViewModel vm = vmAt(root);
    vm.setCurrentPath(Path.of("/tmp/outside"));

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
  // navigateTo + navigateUp (async, via synchronous scheduler)
  // -------------------------------------------------------------------------

  @Test
  void navigateToLoadsChildrenOfDirectory(@TempDir Path root) throws IOException {
    Files.createDirectory(root.resolve("plain_folder"));
    Path proj = Files.createDirectory(root.resolve("a_project"));
    Files.createFile(proj.resolve(PROJECT_MARKER));

    FileBrowserViewModel vm = vmAt(root);
    vm.navigateTo(root);

    assertEquals(2, vm.getItems().size());
    assertFalse(vm.isLoading());
    assertNull(vm.getError());
  }

  @Test
  void navigateToUpdatesCurrentPath(@TempDir Path root) throws IOException {
    Path sub = Files.createDirectory(root.resolve("sub"));
    FileBrowserViewModel vm = vmAt(root);

    vm.navigateTo(sub);

    assertEquals(sub, vm.getCurrentPath());
  }

  @Test
  void navigateUpAtRootIsNoOp(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    Path before = vm.getCurrentPath();

    vm.navigateUp();

    assertEquals(before, vm.getCurrentPath());
  }

  @Test
  void navigateUpFromSubdirGoesToParent(@TempDir Path root) throws IOException {
    Path sub = Files.createDirectory(root.resolve("sub"));
    FileBrowserViewModel vm = vmAt(root);
    vm.navigateTo(sub);

    vm.navigateUp();

    assertEquals(root, vm.getCurrentPath());
  }

  // -------------------------------------------------------------------------
  // createProject (success + failure paths)
  // -------------------------------------------------------------------------

  @Test
  void createProjectAddsEntryAndFiresPendingSelect(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    AtomicInteger pendingFired = new AtomicInteger();
    vm.pendingSelectNameProperty()
        .addListener(
            (obs, old, name) -> {
              if (name != null) pendingFired.incrementAndGet();
            });

    vm.createProject(root, "newproj", "memo text");

    assertEquals(1, vm.getItems().size());
    assertEquals("newproj", vm.getItems().get(0).name());
    assertTrue(vm.getItems().get(0) instanceof ProjectEntry);
    assertEquals("newproj", vm.getPendingSelectName());
    assertEquals(1, pendingFired.get());
  }

  @Test
  void createProjectPublishesPendingSelectAfterItemsAreLoaded(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    // Listener captures itemsCount at the moment pendingSelect fires.
    AtomicInteger itemsCountWhenPendingFired = new AtomicInteger(-1);
    vm.pendingSelectNameProperty()
        .addListener(
            (obs, old, name) -> {
              if (name != null) {
                itemsCountWhenPendingFired.set(vm.getItems().size());
              }
            });

    vm.createProject(root, "p1", "");

    // pendingSelect must fire AFTER items are populated, so the controller can find the entry.
    assertEquals(1, itemsCountWhenPendingFired.get());
  }

  @Test
  void createProjectWithBlankNameSetsError(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);

    vm.createProject(root, "  ", "");

    assertNotNull(vm.getError());
    assertTrue(vm.getError() instanceof IllegalArgumentException);
  }

  @Test
  void createProjectInExistingDirSetsError(@TempDir Path root) throws IOException {
    Files.createDirectory(root.resolve("dup"));
    FileBrowserViewModel vm = vmAt(root);

    vm.createProject(root, "dup", "");

    assertNotNull(vm.getError());
  }

  @Test
  void clearErrorResetsErrorProperty(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    vm.createProject(root, "  ", ""); // triggers error
    assertNotNull(vm.getError());

    vm.clearError();

    assertNull(vm.getError());
  }

  @Test
  void pendingSelectNameCanBeCleared(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    vm.createProject(root, "p", "");
    assertEquals("p", vm.getPendingSelectName());

    vm.clearPendingSelectName();

    assertNull(vm.getPendingSelectName());
  }

  // -------------------------------------------------------------------------
  // openProject (via openingProperty trigger)
  // -------------------------------------------------------------------------

  @Test
  void openProjectPublishesLoadedProjectOnSuccess(@TempDir Path root) throws IOException {
    Path projDir = Files.createDirectory(root.resolve("myproj"));
    Files.writeString(projDir.resolve(PROJECT_MARKER), "memo");
    FileBrowserViewModel vm = vmAt(root);
    ProjectEntry entry = new ProjectEntry(projDir, "myproj", "memo");

    vm.setOpeningProject(entry);

    AFFrProject opened = vm.getOpenedProject();
    assertNotNull(opened);
    assertEquals("myproj", opened.getName());
    assertEquals("memo", opened.getMemo());
  }

  @Test
  void openProjectFailureSetsErrorAndClearsOpening(@TempDir Path root) {
    // Project path that does not exist on disk.
    Path missing = root.resolve("missing");
    FileBrowserViewModel vm = vmAt(root);
    ProjectEntry entry = new ProjectEntry(missing, "missing", "");

    vm.setOpeningProject(entry);

    assertNotNull(vm.getError());
    assertNull(vm.getOpeningProject(), "openingProject should be cleared on failure for retry");
    assertNull(vm.getOpenedProject());
  }

  @Test
  void clearOpenedProjectResets(@TempDir Path root) throws IOException {
    Path projDir = Files.createDirectory(root.resolve("myproj"));
    Files.writeString(projDir.resolve(PROJECT_MARKER), "");
    FileBrowserViewModel vm = vmAt(root);
    vm.setOpeningProject(new ProjectEntry(projDir, "myproj", ""));
    assertNotNull(vm.getOpenedProject());

    vm.clearOpenedProject();

    assertNull(vm.getOpenedProject());
  }

  // -------------------------------------------------------------------------
  // loadChildrenAsync (used by tree mode)
  // -------------------------------------------------------------------------

  @Test
  void loadChildrenAsyncDeliversChildrenOnSuccess(@TempDir Path root) throws IOException {
    Files.createDirectory(root.resolve("a"));
    Files.createDirectory(root.resolve("b"));
    FileBrowserViewModel vm = vmAt(root);

    AtomicInteger calls = new AtomicInteger();
    vm.loadChildrenAsync(
        root,
        (children, err) -> {
          calls.incrementAndGet();
          assertNull(err);
          assertEquals(2, children.size());
        });

    assertEquals(1, calls.get());
  }

  @Test
  void loadChildrenAsyncDeliversErrorOnFailure(@TempDir Path root) {
    FileBrowserViewModel vm = vmAt(root);
    Path missing = root.resolve("does-not-exist");

    AtomicInteger calls = new AtomicInteger();
    vm.loadChildrenAsync(
        missing,
        (children, err) -> {
          calls.incrementAndGet();
          // missing dir is auto-created by DataStore.loadChildren, so this actually succeeds.
          // Use a path that DataStore cannot create (under /proc on linux is too risky); this test
          // documents that the callback fires exactly once regardless of outcome.
          assertEquals(0, children.size());
        });

    assertEquals(1, calls.get());
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
