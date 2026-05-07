package affr.util.prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  private Locale locale;

  private UserPreferences(Locale locale) {
    this.locale = locale;
  }

  /**
   * Loads preferences from {@code ~/.affr/preferences.properties}. Returns defaults if the file
   * is absent or unreadable.
   */
  public static UserPreferences load() {
    Properties props = new Properties();
    if (Files.exists(PREFS_FILE)) {
      try (InputStream in = Files.newInputStream(PREFS_FILE)) {
        props.load(in);
      } catch (IOException ignored) {
      }
    }
    return new UserPreferences(parseLocale(props.getProperty(KEY_LANGUAGE)));
  }

  /** Returns the saved UI locale. */
  public Locale locale() {
    return locale;
  }

  /** Updates the in-memory locale. Call {@link #save()} to persist the change. */
  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  /** Writes current preferences to {@code ~/.affr/preferences.properties}. */
  public void save() {
    try {
      Files.createDirectories(APP_DIR);
      Properties props = new Properties();
      props.setProperty(KEY_LANGUAGE, locale.getLanguage());
      try (OutputStream out = Files.newOutputStream(PREFS_FILE)) {
        props.store(out, "AFFr user preferences — do not edit while the app is running");
      }
    } catch (IOException ignored) {
    }
  }

  private static Locale parseLocale(@Nullable String tag) {
    if (tag == null || tag.isBlank()) return DEFAULT_LOCALE;
    return switch (tag.toLowerCase(Locale.ROOT)) {
      case "ja" -> Locale.JAPANESE;
      default -> Locale.ENGLISH;
    };
  }
}
