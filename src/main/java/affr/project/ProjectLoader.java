package affr.project;

import affr.util.fs.FsLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Loads an {@link AFFrProject} from disk.
 *
 * <p>All methods perform IO and must be called from a background thread — never from the JavaFX
 * Application Thread.
 *
 * <p>Item discovery uses marker files: a child directory containing {@code .affr_property} is an
 * {@link AFFrCalculation}. Future item types add their own marker check here and a new {@code
 * permits} entry in {@link ProjectItem}.
 */
public final class ProjectLoader {

  private static final String PROJECT_MARKER = ".affr_project";
  private static final String CAL_PROPERTY = ".affr_property";
  private static final String CAL_MODE = ".mode";

  /**
   * Loads the project rooted at {@code projectPath}.
   *
   * @param projectPath absolute path to the project directory (must contain {@code .affr_project})
   * @return the loaded project; never {@code null}
   * @throws IOException if the directory cannot be listed
   */
  public AFFrProject load(Path projectPath) throws IOException {
    @Nullable Path fn = projectPath.getFileName();
    String name = fn != null ? fn.toString() : projectPath.toString();
    String memo = readMemo(projectPath);
    List<AFFrCalculation> calculations = loadCalculations(projectPath);
    AFFrProject project = new AFFrProject(name, projectPath, memo, calculations);
    for (AFFrCalculation cal : calculations) {
      cal.setProject(project);
    }
    return project;
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private List<AFFrCalculation> loadCalculations(Path projectPath) throws IOException {
    List<AFFrCalculation> result = new ArrayList<>();
    for (Path child : FsLoader.listChildDirs(projectPath)) {
      if (Files.exists(child.resolve(CAL_PROPERTY))) {
        result.add(loadCalculation(child));
      }
      // Future: check .affr_mesh → MeshGeneratorItem, .affr_optimizer → OptimizerItem, etc.
    }
    return result;
  }

  private static AFFrCalculation loadCalculation(Path path) {
    @Nullable Path fn = path.getFileName();
    String name = fn != null ? fn.toString() : path.toString();
    AFFrCalProperty property = loadCalProperty(path);
    AFFrCalculationModel model = loadCalModel(path);
    // project back-reference is set by the caller (load()) after AFFrProject is constructed.
    return new AFFrCalculation(name, path, null, property, model);
  }

  // ── .affr_property ────────────────────────────────────────────────────────

  private static AFFrCalProperty loadCalProperty(Path calPath) {
    Path file = calPath.resolve(CAL_PROPERTY);
    try {
      String json = Files.readString(file);
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

      CalculationStatus status = parseStatus(getString(obj, "status", null));
      String date = requireString(obj, "date", "");
      int timeStep = getInt(obj, "timeStep", 0);
      String host = requireString(obj, "host", "localhost");
      String jobId = requireString(obj, "jobId", "");
      String queueName = requireString(obj, "queueName", "未設定");
      int ncpu = getInt(obj, "ncpu", 1);
      boolean userSubrtUsed = getBool(obj, "userSubrtUsed", false);
      Map<String, String> execFiles = parseStringMap(obj, "execFiles");
      Map<String, Boolean> usrsubCheck = parseBoolMap(obj, "usrsubCheck");

      return new AFFrCalProperty(
          status,
          date,
          timeStep,
          host,
          jobId,
          queueName,
          ncpu,
          userSubrtUsed,
          execFiles,
          usrsubCheck);
    } catch (IOException | JsonSyntaxException | IllegalStateException ignored) {
      return AFFrCalProperty.DEFAULT;
    }
  }

  private static CalculationStatus parseStatus(@Nullable String value) {
    if (value == null) return CalculationStatus.SETTING;
    try {
      return CalculationStatus.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException ignored) {
      return CalculationStatus.SETTING;
    }
  }

  // ── .mode ─────────────────────────────────────────────────────────────────

  private static AFFrCalculationModel loadCalModel(Path calPath) {
    Path file = calPath.resolve(CAL_MODE);
    try {
      String json = Files.readString(file);
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

      ComprsModel comprs =
          parseEnum(
              ComprsModel.class,
              getString(obj, "comprsModel", null),
              AFFrCalculationModel.DEFAULT.comprsModel());
      SteadyModel steady =
          parseEnum(
              SteadyModel.class,
              getString(obj, "steadyModel", null),
              AFFrCalculationModel.DEFAULT.steadyModel());
      TurbModel turb =
          parseEnum(
              TurbModel.class,
              getString(obj, "turbModel", null),
              AFFrCalculationModel.DEFAULT.turbModel());
      Set<ExtraModel> extras = parseExtraModels(obj);

      return new AFFrCalculationModel(comprs, steady, turb, extras);
    } catch (IOException | JsonSyntaxException | IllegalStateException ignored) {
      return AFFrCalculationModel.DEFAULT;
    }
  }

  private static Set<ExtraModel> parseExtraModels(JsonObject obj) {
    if (!obj.has("extraModelSet")) return Set.of();
    try {
      JsonArray arr = obj.getAsJsonArray("extraModelSet");
      Set<ExtraModel> result = EnumSet.noneOf(ExtraModel.class);
      for (JsonElement el : arr) {
        try {
          result.add(ExtraModel.valueOf(el.getAsString().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
          // Skip unrecognised entries.
        }
      }
      return result.isEmpty() ? Set.of() : Set.copyOf(result);
    } catch (ClassCastException ignored) {
      return Set.of();
    }
  }

  // ── JSON helpers ──────────────────────────────────────────────────────────

  private static @Nullable String getString(JsonObject obj, String key, @Nullable String def) {
    if (!obj.has(key)) return def;
    try {
      return obj.get(key).getAsString();
    } catch (ClassCastException | IllegalStateException ignored) {
      return def;
    }
  }

  /** Variant of {@link #getString} that guarantees a non-null result via a non-null default. */
  private static String requireString(JsonObject obj, String key, String def) {
    if (!obj.has(key)) return def;
    try {
      return obj.get(key).getAsString();
    } catch (ClassCastException | IllegalStateException ignored) {
      return def;
    }
  }

  private static int getInt(JsonObject obj, String key, int def) {
    if (!obj.has(key)) return def;
    try {
      return obj.get(key).getAsInt();
    } catch (ClassCastException | NumberFormatException | IllegalStateException ignored) {
      return def;
    }
  }

  private static boolean getBool(JsonObject obj, String key, boolean def) {
    if (!obj.has(key)) return def;
    try {
      return obj.get(key).getAsBoolean();
    } catch (ClassCastException | IllegalStateException ignored) {
      return def;
    }
  }

  private static Map<String, String> parseStringMap(JsonObject obj, String key) {
    if (!obj.has(key)) return Map.of();
    try {
      JsonObject map = obj.getAsJsonObject(key);
      Map<String, String> result = new HashMap<>();
      for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
        result.put(entry.getKey(), entry.getValue().getAsString());
      }
      return Map.copyOf(result);
    } catch (ClassCastException | IllegalStateException ignored) {
      return Map.of();
    }
  }

  private static Map<String, Boolean> parseBoolMap(JsonObject obj, String key) {
    if (!obj.has(key)) return Map.of();
    try {
      JsonObject map = obj.getAsJsonObject(key);
      Map<String, Boolean> result = new HashMap<>();
      for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
        result.put(entry.getKey(), entry.getValue().getAsBoolean());
      }
      return Map.copyOf(result);
    } catch (ClassCastException | IllegalStateException ignored) {
      return Map.of();
    }
  }

  private static <E extends Enum<E>> E parseEnum(
      Class<E> type, @Nullable String value, E fallback) {
    if (value == null) return fallback;
    try {
      return Enum.valueOf(type, value.toUpperCase());
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }

  // ── Project memo ──────────────────────────────────────────────────────────

  private static String readMemo(Path projectPath) {
    Path marker = projectPath.resolve(PROJECT_MARKER);
    try {
      return Files.readString(marker).strip();
    } catch (IOException ignored) {
      return "";
    }
  }
}
