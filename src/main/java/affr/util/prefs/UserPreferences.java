package affr.util.prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Per-user persistent preferences stored at {@code ~/.affr/preferences.properties}.
 *
 * <p>The root directory {@code ~/.affr/} is the application's home for all user-specific data.
 * Settings here are not limited to the UI layer — any layer (services, runners, etc.) may read
 * preferences without depending on {@code affr.app}.
 *
 * <p>IO errors (unreadable file, missing parent directory, etc.) are treated as "use defaults" on
 * load and silently ignored on save — preferences are best-effort, not critical data.
 */
public final class UserPreferences {

  /** Root application directory in the user's home folder. */
  public static final Path APP_DIR = Path.of(System.getProperty("user.home"), ".affr");

  private static final Path PREFS_FILE = APP_DIR.resolve("preferences.properties");

  private static final String KEY_LANGUAGE = "language";
  private static final String KEY_WINDOW_WIDTH = "window.width";
  private static final String KEY_WINDOW_HEIGHT = "window.height";
  private static final String KEY_WINDOW_X = "window.x";
  private static final String KEY_WINDOW_Y = "window.y";
  private static final String KEY_BROWSER_VIEW_MODE = "browser.viewMode";
  private static final String KEY_BROWSER_PATH = "browser.path";
  private static final String KEY_GUI_INSTALL_PATH = "gui.installPath";
  private static final String KEY_SOLVER_INSTALL_PATH = "solver.installPath";

  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
  public static final double DEFAULT_WINDOW_WIDTH = 1200;
  public static final double DEFAULT_WINDOW_HEIGHT = 720;

  private final Path prefsFile;
  private Locale locale;
  private double windowWidth;
  private double windowHeight;

  /** {@code NaN} means no saved position — the OS should place the window. */
  private double windowX;

  private double windowY;

  /** {@code null} means use the default view mode (LIST). */
  private @Nullable String browserViewMode;

  /** {@code null} means start at the workspace root. */
  private @Nullable Path browserPath;

  /**
   * Root directory of the AFFr GUI installation (e.g. {@code C:\Program
   * Files\Advancesoft\AFFrGUI}). {@code null} means neither explicitly configured nor a known
   * platform default. When set, the tutorial set is resolved as {@code guiInstallPath/tutorials/}.
   */
  private @Nullable Path guiInstallPath;

  /**
   * Root directory of the AFFr solver installation (e.g. {@code C:\Program
   * Files\Advancesoft\AFFr}). {@code null} means neither explicitly configured nor a known platform
   * default.
   */
  private @Nullable Path solverInstallPath;

  private UserPreferences(
      Path prefsFile,
      Locale locale,
      double windowWidth,
      double windowHeight,
      double windowX,
      double windowY,
      @Nullable String browserViewMode,
      @Nullable Path browserPath,
      @Nullable Path guiInstallPath,
      @Nullable Path solverInstallPath) {
    this.prefsFile = prefsFile;
    this.locale = locale;
    this.windowWidth = windowWidth;
    this.windowHeight = windowHeight;
    this.windowX = windowX;
    this.windowY = windowY;
    this.browserViewMode = browserViewMode;
    this.browserPath = browserPath;
    this.guiInstallPath = guiInstallPath;
    this.solverInstallPath = solverInstallPath;
  }

  /**
   * Loads preferences from {@code ~/.affr/preferences.properties}. Returns defaults if the file is
   * absent or unreadable.
   */
  public static UserPreferences load() {
    return loadFrom(PREFS_FILE);
  }

  /**
   * Loads preferences from the given {@code prefsFile}. Returns defaults if the file is absent or
   * unreadable. Exposed for tests that need to supply a temporary file.
   */
  public static UserPreferences loadFrom(Path prefsFile) {
    Properties props = new Properties();
    if (Files.exists(prefsFile)) {
      try (InputStream in = Files.newInputStream(prefsFile)) {
        props.load(in);
      } catch (IOException ignored) {
      }
    }
    @Nullable Path storedGuiPath = parsePath(props.getProperty(KEY_GUI_INSTALL_PATH));
    @Nullable Path storedSolverPath = parsePath(props.getProperty(KEY_SOLVER_INSTALL_PATH));
    return new UserPreferences(
        prefsFile,
        parseLocale(props.getProperty(KEY_LANGUAGE)),
        parsePositiveDouble(props.getProperty(KEY_WINDOW_WIDTH), DEFAULT_WINDOW_WIDTH),
        parsePositiveDouble(props.getProperty(KEY_WINDOW_HEIGHT), DEFAULT_WINDOW_HEIGHT),
        parseFiniteDouble(props.getProperty(KEY_WINDOW_X)),
        parseFiniteDouble(props.getProperty(KEY_WINDOW_Y)),
        props.getProperty(KEY_BROWSER_VIEW_MODE),
        parsePath(props.getProperty(KEY_BROWSER_PATH)),
        storedGuiPath != null ? storedGuiPath : defaultGuiInstallPath(),
        storedSolverPath != null ? storedSolverPath : defaultSolverInstallPath());
  }

  /** Returns the saved UI locale. */
  public Locale locale() {
    return locale;
  }

  /** Updates the in-memory locale. Call {@link #save()} to persist the change. */
  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /** Returns the saved main-window width in pixels. */
  public double windowWidth() {
    return windowWidth;
  }

  /** Returns the saved main-window height in pixels. */
  public double windowHeight() {
    return windowHeight;
  }

  /**
   * Returns the saved main-window X position, or {@link Double#NaN} if no position has been saved
   * yet (the OS should choose the initial placement).
   */
  public double windowX() {
    return windowX;
  }

  /**
   * Returns the saved main-window Y position, or {@link Double#NaN} if no position has been saved
   * yet (the OS should choose the initial placement).
   */
  public double windowY() {
    return windowY;
  }

  /** Updates the in-memory window size. Call {@link #save()} to persist the change. */
  public void setWindowSize(double width, double height) {
    this.windowWidth = width;
    this.windowHeight = height;
  }

  /**
   * Updates the in-memory window position. Use {@link Double#NaN} for either coordinate to let the
   * OS decide that axis. Call {@link #save()} to persist the change.
   */
  public void setWindowPosition(double x, double y) {
    this.windowX = x;
    this.windowY = y;
  }

  /**
   * Returns the saved file-browser view mode name (e.g. {@code "LIST"}, {@code "ICON"}, {@code
   * "TREE"}), or {@code null} if none has been saved yet.
   */
  public @Nullable String browserViewMode() {
    return browserViewMode;
  }

  /** Updates the in-memory browser view mode name. Call {@link #save()} to persist the change. */
  public void setBrowserViewMode(@Nullable String mode) {
    this.browserViewMode = mode;
  }

  /**
   * Returns the saved file-browser directory path, or {@code null} if none has been saved yet (the
   * browser will start at the workspace root).
   */
  public @Nullable Path browserPath() {
    return browserPath;
  }

  /** Updates the in-memory browser path. Call {@link #save()} to persist the change. */
  public void setBrowserPath(@Nullable Path path) {
    this.browserPath = path;
  }

  /**
   * Returns the GUI installation directory, or {@code null} if neither explicitly configured nor a
   * known platform default exists. The tutorial set is resolved as {@code
   * guiInstallPath().resolve("tutorials")}.
   */
  public @Nullable Path guiInstallPath() {
    return guiInstallPath;
  }

  /** Updates the in-memory GUI installation path. Call {@link #save()} to persist the change. */
  public void setGuiInstallPath(@Nullable Path path) {
    this.guiInstallPath = path;
  }

  /**
   * Returns the solver installation directory, or {@code null} if neither explicitly configured nor
   * a known platform default exists.
   */
  public @Nullable Path solverInstallPath() {
    return solverInstallPath;
  }

  /** Updates the in-memory solver installation path. Call {@link #save()} to persist the change. */
  public void setSolverInstallPath(@Nullable Path path) {
    this.solverInstallPath = path;
  }

  /**
   * Returns the platform-specific default GUI installation path, or {@code null} on platforms where
   * no standard location is known.
   *
   * <ul>
   *   <li>Windows: {@code C:\Program Files\Advancesoft\AFFrGUI}
   * </ul>
   */
  public static @Nullable Path defaultGuiInstallPath() {
    if (isWindows()) {
      return Path.of("C:\\Program Files\\Advancesoft\\AFFrGUI");
    }
    return null;
  }

  /**
   * Returns the platform-specific default solver installation path, or {@code null} on platforms
   * where no standard location is known.
   *
   * <ul>
   *   <li>Windows: {@code C:\Program Files\Advancesoft\AFFr}
   * </ul>
   */
  public static @Nullable Path defaultSolverInstallPath() {
    if (isWindows()) {
      return Path.of("C:\\Program Files\\Advancesoft\\AFFr");
    }
    return null;
  }

  /** Writes current preferences to the file this instance was loaded from. */
  public void save() {
    try {
      Path parent = prefsFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Properties props = new Properties();
      props.setProperty(KEY_LANGUAGE, locale.getLanguage());
      props.setProperty(KEY_WINDOW_WIDTH, String.valueOf(windowWidth));
      props.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(windowHeight));
      if (Double.isFinite(windowX)) props.setProperty(KEY_WINDOW_X, String.valueOf(windowX));
      if (Double.isFinite(windowY)) props.setProperty(KEY_WINDOW_Y, String.valueOf(windowY));
      if (browserViewMode != null) props.setProperty(KEY_BROWSER_VIEW_MODE, browserViewMode);
      if (browserPath != null) {
        props.setProperty(KEY_BROWSER_PATH, browserPath.toAbsolutePath().toString());
      }
      if (guiInstallPath != null) {
        props.setProperty(KEY_GUI_INSTALL_PATH, guiInstallPath.toAbsolutePath().toString());
      }
      if (solverInstallPath != null) {
        props.setProperty(KEY_SOLVER_INSTALL_PATH, solverInstallPath.toAbsolutePath().toString());
      }
      try (OutputStream out = Files.newOutputStream(prefsFile)) {
        props.store(out, "AFFr user preferences — do not edit while the app is running");
      }
    } catch (IOException ignored) {
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }

  private static Locale parseLocale(@Nullable String tag) {
    if (tag == null || tag.isBlank()) return DEFAULT_LOCALE;
    return switch (tag.toLowerCase(Locale.ROOT)) {
      case "ja" -> Locale.JAPANESE;
      default -> Locale.ENGLISH;
    };
  }

  /** Parses a positive-only double (used for width/height). Returns {@code fallback} on failure. */
  private static double parsePositiveDouble(@Nullable String value, double fallback) {
    if (value == null || value.isBlank()) return fallback;
    try {
      double d = Double.parseDouble(value);
      return Double.isFinite(d) && d > 0 ? d : fallback;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  /**
   * Parses any finite double (used for X/Y position, which may be negative on multi-monitor
   * setups). Returns {@link Double#NaN} on failure or if the value is not finite.
   */
  private static double parseFiniteDouble(@Nullable String value) {
    if (value == null || value.isBlank()) return Double.NaN;
    try {
      double d = Double.parseDouble(value);
      return Double.isFinite(d) ? d : Double.NaN;
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }

  /**
   * Parses a persisted path value. Returns {@code null} if the value is absent, blank, or not a
   * syntactically valid path on this filesystem.
   */
  private static @Nullable Path parsePath(@Nullable String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Path.of(value);
    } catch (InvalidPathException e) {
      return null;
    }
  }
}
