package affr.project;

import affr.util.fs.FsLoader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Loads an {@link AFFrProject} from disk.
 *
 * <p>All methods perform IO and must be called from a background thread — never from the JavaFX
 * Application Thread.
 *
 * <p>Item discovery uses marker files: a child directory containing {@code .affr_property} is a
 * {@link CalculationItem}. Future item types add their own marker check here and a new {@code
 * permits} entry in {@link ProjectItem}.
 */
public final class ProjectLoader {

  private static final String PROJECT_MARKER = ".affr_project";
  private static final String CAL_PROPERTY = ".affr_property";

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
    List<ProjectItem> items = loadItems(projectPath);
    return new AFFrProject(name, projectPath, memo, items);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private List<ProjectItem> loadItems(Path projectPath) throws IOException {
    List<ProjectItem> result = new ArrayList<>();
    for (Path child : FsLoader.listChildDirs(projectPath)) {
      Path calMarker = child.resolve(CAL_PROPERTY);
      if (Files.exists(calMarker)) {
        result.add(loadCalculationItem(child));
      }
      // Future: check .affr_mesh → MeshItem, .affr_survey → ParameterSurveyItem, etc.
    }
    return result;
  }

  private static CalculationItem loadCalculationItem(Path path) {
    @Nullable Path fn = path.getFileName();
    String name = fn != null ? fn.toString() : path.toString();
    Path propertyFile = path.resolve(CAL_PROPERTY);
    CalculationStatus status = CalculationStatus.SETTING;
    String date = "";
    try {
      String json = Files.readString(propertyFile);
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      if (obj.has("status")) {
        status = parseStatus(obj.get("status").getAsString());
      }
      if (obj.has("date")) {
        date = obj.get("date").getAsString();
      }
    } catch (IOException | JsonSyntaxException | IllegalStateException ignored) {
      // Fall back to defaults: SETTING status, empty date.
    }
    return new CalculationItem(name, path, status, date);
  }

  private static CalculationStatus parseStatus(String value) {
    try {
      return CalculationStatus.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException ignored) {
      return CalculationStatus.SETTING;
    }
  }

  private static String readMemo(Path projectPath) {
    Path marker = projectPath.resolve(PROJECT_MARKER);
    try {
      return Files.readString(marker).strip();
    } catch (IOException ignored) {
      return "";
    }
  }
}
