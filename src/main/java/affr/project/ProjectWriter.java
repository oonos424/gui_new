package affr.project;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Creates and modifies project items on disk.
 *
 * <p>All methods perform IO and must be called from a background thread — never from the JavaFX
 * Application Thread.
 *
 * <p>This class is the write-side counterpart of {@link ProjectLoader}.
 */
public final class ProjectWriter {

  private static final String CAL_PROPERTY = ".affr_property";
  private static final String CAL_MODE = ".mode";

  private ProjectWriter() {}

  /**
   * Creates a new calculation inside {@code projectPath} with the next available sequential name
   * ({@code cal_01}, {@code cal_02}, …) and default property and model files.
   *
   * @param projectPath absolute path to the project directory
   * @param existingItems the current items in the project; used to determine the next name
   * @param project the owning project (set as the back-reference on the new calculation)
   * @return the newly created {@link AFFrCalculation}
   * @throws IOException if the directory or any file cannot be created
   */
  public static AFFrCalculation createCalculation(
      Path projectPath, List<? extends ProjectItem> existingItems, AFFrProject project)
      throws IOException {
    String name = nextCalName(existingItems);
    Path calDir = projectPath.resolve(name);
    if (Files.exists(calDir)) {
      throw new IOException("'" + name + "' already exists in this project");
    }
    Files.createDirectory(calDir);
    writeCalProperty(calDir, AFFrCalProperty.DEFAULT);
    writeCalModel(calDir, AFFrCalculationModel.DEFAULT);
    return new AFFrCalculation(
        name, calDir, project, AFFrCalProperty.DEFAULT, AFFrCalculationModel.DEFAULT);
  }

  // ── Name generation ────────────────────────────────────────────────────────

  static String nextCalName(List<? extends ProjectItem> items) {
    int max =
        items.stream()
            .map(ProjectItem::name)
            .filter(n -> n.matches("cal_\\d+"))
            .mapToInt(n -> Integer.parseInt(n.substring(4)))
            .max()
            .orElse(0);
    return String.format("cal_%02d", max + 1);
  }

  // ── File writers ───────────────────────────────────────────────────────────

  /**
   * Writes {@code .affr_property} from the given {@link AFFrCalProperty} into {@code calDir}.
   *
   * @throws IOException if the file cannot be written
   */
  public static void writeCalProperty(Path calDir, AFFrCalProperty p) throws IOException {
    JsonObject obj = new JsonObject();
    obj.addProperty("status", p.status().name());
    obj.addProperty("date", p.date());
    obj.addProperty("timeStep", p.timeStep());
    obj.addProperty("host", p.host());
    obj.addProperty("jobId", p.jobId());
    obj.addProperty("queueName", p.queueName());
    obj.addProperty("ncpu", p.ncpu());
    obj.addProperty("userSubrtUsed", p.userSubrtUsed());

    JsonObject execFiles = new JsonObject();
    p.execFiles().forEach(execFiles::addProperty);
    obj.add("execFiles", execFiles);

    JsonObject usrsubCheck = new JsonObject();
    p.usrsubCheck().forEach(usrsubCheck::addProperty);
    obj.add("usrsubCheck", usrsubCheck);

    Files.writeString(
        calDir.resolve(CAL_PROPERTY), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
  }

  /**
   * Writes {@code .mode} from the given {@link AFFrCalculationModel} into {@code calDir}.
   *
   * @throws IOException if the file cannot be written
   */
  public static void writeCalModel(Path calDir, AFFrCalculationModel m) throws IOException {
    JsonObject obj = new JsonObject();
    obj.addProperty("comprsModel", m.comprsModel().name());
    obj.addProperty("steadyModel", m.steadyModel().name());
    obj.addProperty("turbModel", m.turbModel().name());

    JsonArray extras = new JsonArray();
    m.extraModelSet().forEach(e -> extras.add(e.name()));
    obj.add("extraModelSet", extras);

    Files.writeString(
        calDir.resolve(CAL_MODE), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
  }
}
