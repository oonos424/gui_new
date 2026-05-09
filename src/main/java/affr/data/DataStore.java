package affr.data;

import affr.util.fs.FsLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Master data controller for the AFFr workspace.
 *
 * <p>Owns the root workspace path and is responsible for loading the browser-level view of the
 * filesystem as typed domain objects ({@link BrowserEntry}). It is the single place in the codebase
 * where raw filesystem paths are interpreted as AFFr domain concepts (projects vs. plain folders).
 *
 * <p>All methods that perform IO must be called from a background thread — never from the JavaFX
 * Application Thread. Observable state (item lists, loading flags) lives in the ViewModel layer;
 * {@code DataStore} is purely a synchronous data-access service.
 *
 * <p>A single {@code DataStore} instance is constructed at application startup and shared across
 * all views that need workspace data.
 */
public final class DataStore {

  /** The hidden file whose presence marks a directory as an AFFr project. */
  private static final String PROJECT_MARKER = ".affr_project";

  private final Path rootPath;

  public DataStore(Path rootPath) {
    this.rootPath = rootPath;
  }

  /** The fixed workspace root — the browser cannot navigate above this path. */
  public Path getRootPath() {
    return rootPath;
  }

  /**
   * Lists the direct child entries of {@code dir} as typed domain objects, sorted so that {@link
   * ProjectEntry} items come before {@link FolderEntry} items; within each group entries are sorted
   * case-insensitively by name.
   *
   * <p>If {@code dir} does not exist it is created before listing (first-run initialisation).
   *
   * @param dir the directory to list; normally the workspace root or a sub-folder
   * @return immutable list of browser entries; empty if the directory contains no visible
   *     sub-directories
   * @throws IOException if the directory cannot be created or listed
   */
  public List<BrowserEntry> loadChildren(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      Files.createDirectories(dir);
    }
    return FsLoader.listChildDirs(dir).stream()
        .map(this::toBrowserEntry)
        .sorted(
            Comparator.comparing((BrowserEntry e) -> !(e instanceof ProjectEntry))
                .thenComparing(BrowserEntry::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /**
   * Creates a new AFFr project inside {@code parentDir}.
   *
   * <p>Creates {@code parentDir/name/} and writes the {@code .affr_project} marker file with {@code
   * memo} as its content. The directory must not already exist.
   *
   * @param parentDir the directory in which to create the project; must already exist
   * @param name directory name; must not be blank or contain path separators
   * @param memo optional free-text memo written verbatim to {@code .affr_project}
   * @return the newly created {@link ProjectEntry}
   * @throws IOException if the directory already exists or any other filesystem error occurs
   * @throws IllegalArgumentException if {@code name} is blank or contains a path separator
   */
  public ProjectEntry createProject(Path parentDir, String name, String memo) throws IOException {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Project name must not be blank");
    }
    if (name.contains("/") || name.contains("\\")) {
      throw new IllegalArgumentException("Project name must not contain path separators");
    }
    Path projectDir = parentDir.resolve(name);
    if (Files.exists(projectDir)) {
      throw new IOException("'" + name + "' already exists in this directory");
    }
    Files.createDirectory(projectDir);
    Files.writeString(projectDir.resolve(PROJECT_MARKER), memo);
    return new ProjectEntry(projectDir, name, memo.strip());
  }

  private BrowserEntry toBrowserEntry(Path path) {
    Path fn = path.getFileName();
    String name = fn != null ? fn.toString() : path.toString();
    Path marker = path.resolve(PROJECT_MARKER);
    if (Files.exists(marker)) {
      return new ProjectEntry(path, name, readMemo(marker));
    }
    return new FolderEntry(path, name);
  }

  /**
   * Reads the memo text from a {@code .affr_project} file. Returns an empty string if the file is
   * unreadable or contains only whitespace.
   */
  private static String readMemo(Path markerFile) {
    try {
      return Files.readString(markerFile).strip();
    } catch (IOException ignored) {
      return "";
    }
  }
}
