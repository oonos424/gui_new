package affr.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link DataStore}.
 *
 * <p>These are the highest-value tests in the data layer: {@code DataStore} is the single place
 * where raw filesystem paths are interpreted as AFFr domain concepts, so every rule it enforces
 * (project detection, sort order, memo reading, root creation) is verified here.
 *
 * <p>All tests use a JUnit {@link TempDir} for full isolation; they never touch {@code ~/.affr/}.
 */
final class DataStoreTest {

  private static final String PROJECT_MARKER = ".affr_project";

  // -------------------------------------------------------------------------
  // Entry-type classification
  // -------------------------------------------------------------------------

  @Test
  void plainDirWithNoMarkerBecomesFolderEntry(@TempDir Path root) throws IOException {
    Files.createDirectory(root.resolve("plain_folder"));
    DataStore store = new DataStore(root);

    List<BrowserEntry> entries = store.loadChildren(root);

    assertEquals(1, entries.size());
    assertInstanceOf(FolderEntry.class, entries.get(0));
    assertEquals("plain_folder", entries.get(0).name());
  }

  @Test
  void dirWithMarkerFileBecomeProjectEntry(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("my_project"));
    Files.createFile(proj.resolve(PROJECT_MARKER));
    DataStore store = new DataStore(root);

    List<BrowserEntry> entries = store.loadChildren(root);

    assertEquals(1, entries.size());
    assertInstanceOf(ProjectEntry.class, entries.get(0));
    assertEquals("my_project", entries.get(0).name());
  }

  // -------------------------------------------------------------------------
  // Memo reading
  // -------------------------------------------------------------------------

  @Test
  void projectMemoIsReadFromMarkerFileContent(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.writeString(proj.resolve(PROJECT_MARKER), "This is my project memo");
    DataStore store = new DataStore(root);

    List<BrowserEntry> entries = store.loadChildren(root);

    ProjectEntry entry = (ProjectEntry) entries.get(0);
    assertEquals("This is my project memo", entry.memo());
  }

  @Test
  void memoIsStrippedOfLeadingAndTrailingWhitespace(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.writeString(proj.resolve(PROJECT_MARKER), "  trimmed memo  \n");
    DataStore store = new DataStore(root);

    ProjectEntry entry = (ProjectEntry) store.loadChildren(root).get(0);
    assertEquals("trimmed memo", entry.memo());
  }

  @Test
  void emptyMarkerFileYieldsEmptyMemo(@TempDir Path root) throws IOException {
    Path proj = Files.createDirectory(root.resolve("proj"));
    Files.createFile(proj.resolve(PROJECT_MARKER));
    DataStore store = new DataStore(root);

    ProjectEntry entry = (ProjectEntry) store.loadChildren(root).get(0);
    assertEquals("", entry.memo());
  }

  // -------------------------------------------------------------------------
  // Sort order: projects before folders, case-insensitive within each group
  // -------------------------------------------------------------------------

  @Test
  void projectsSortBeforeFolders(@TempDir Path root) throws IOException {
    Files.createDirectory(root.resolve("aaa_folder"));
    Path proj = Files.createDirectory(root.resolve("zzz_project"));
    Files.createFile(proj.resolve(PROJECT_MARKER));
    DataStore store = new DataStore(root);

    List<BrowserEntry> entries = store.loadChildren(root);

    assertInstanceOf(ProjectEntry.class, entries.get(0), "project should come first");
    assertInstanceOf(FolderEntry.class, entries.get(1), "folder should come second");
  }

  @Test
  void withinProjectGroupSortIsCaseInsensitive(@TempDir Path root) throws IOException {
    for (String name : new String[]{"Zeta", "alpha", "Mango"}) {
      Path p = Files.createDirectory(root.resolve(name));
      Files.createFile(p.resolve(PROJECT_MARKER));
    }
    DataStore store = new DataStore(root);

    List<String> names = store.loadChildren(root).stream().map(BrowserEntry::name).toList();
    assertEquals(List.of("alpha", "Mango", "Zeta"), names);
  }

  @Test
  void withinFolderGroupSortIsCaseInsensitive(@TempDir Path root) throws IOException {
    for (String name : new String[]{"Zeta", "alpha", "Mango"}) {
      Files.createDirectory(root.resolve(name));
    }
    DataStore store = new DataStore(root);

    List<String> names = store.loadChildren(root).stream().map(BrowserEntry::name).toList();
    assertEquals(List.of("alpha", "Mango", "Zeta"), names);
  }

  @Test
  void mixedEntriesSortProjectsFirstThenFoldersBothCaseInsensitive(@TempDir Path root)
      throws IOException {
    Files.createDirectory(root.resolve("b_folder"));
    Files.createDirectory(root.resolve("a_folder"));
    Path p1 = Files.createDirectory(root.resolve("d_project"));
    Files.createFile(p1.resolve(PROJECT_MARKER));
    Path p2 = Files.createDirectory(root.resolve("c_project"));
    Files.createFile(p2.resolve(PROJECT_MARKER));
    DataStore store = new DataStore(root);

    List<String> names = store.loadChildren(root).stream().map(BrowserEntry::name).toList();

    assertEquals(List.of("c_project", "d_project", "a_folder", "b_folder"), names);
  }

  // -------------------------------------------------------------------------
  // Hidden entries excluded
  // -------------------------------------------------------------------------

  @Test
  void hiddenDirsAreExcluded(@TempDir Path root) throws IOException {
    Files.createDirectory(root.resolve(".hidden"));
    Files.createDirectory(root.resolve("visible"));
    DataStore store = new DataStore(root);

    List<BrowserEntry> entries = store.loadChildren(root);

    assertEquals(1, entries.size());
    assertEquals("visible", entries.get(0).name());
  }

  @Test
  void projectMarkerFileItselfIsNotReturnedAsEntry(@TempDir Path root) throws IOException {
    // The marker lives at root/.affr_project — it should not appear as a child entry.
    Files.createFile(root.resolve(PROJECT_MARKER));
    DataStore store = new DataStore(root);

    List<BrowserEntry> entries = store.loadChildren(root);

    assertTrue(entries.isEmpty());
  }

  // -------------------------------------------------------------------------
  // Root creation on first run
  // -------------------------------------------------------------------------

  @Test
  void nonExistentRootIsCreatedOnLoadChildren(@TempDir Path base) throws IOException {
    Path newRoot = base.resolve("workspace");
    DataStore store = new DataStore(newRoot);

    List<BrowserEntry> entries = store.loadChildren(newRoot);

    assertTrue(Files.isDirectory(newRoot), "root directory should have been created");
    assertTrue(entries.isEmpty());
  }

  // -------------------------------------------------------------------------
  // getRootPath
  // -------------------------------------------------------------------------

  @Test
  void getRootPathReturnsConstructorArgument(@TempDir Path root) {
    DataStore store = new DataStore(root);

    assertEquals(root, store.getRootPath());
  }
}
