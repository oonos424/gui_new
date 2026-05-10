package affr.project;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
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
   * ({@code cal_01}, {@code cal_02}, …), default property, and {@link AFFrCalculationModel#DEFAULT
   * default model}.
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
    return createCalculation(projectPath, existingItems, project, AFFrCalculationModel.DEFAULT);
  }

  /**
   * Creates a new calculation inside {@code projectPath} with the next available sequential name
   * ({@code cal_01}, {@code cal_02}, …), default property, and the supplied {@code model}.
   *
   * @param projectPath absolute path to the project directory
   * @param existingItems the current items in the project; used to determine the next name
   * @param project the owning project (set as the back-reference on the new calculation)
   * @param model the physics model selection chosen by the user
   * @return the newly created {@link AFFrCalculation}
   * @throws IOException if the directory or any file cannot be created
   */
  public static AFFrCalculation createCalculation(
      Path projectPath,
      List<? extends ProjectItem> existingItems,
      AFFrProject project,
      AFFrCalculationModel model)
      throws IOException {
    return createCalculation(
        projectPath, existingItems, project, model, nextCalName(existingItems));
  }

  /**
   * Creates a new calculation with the supplied {@code name} (rather than the next sequential
   * default).
   *
   * <p>Validation matches {@link #renameCalculation}: {@code name} must be non-blank after
   * trimming, and must not collide (case-insensitively) with any existing item in {@code
   * existingItems}. The trimmed name is used both as the on-disk directory name and as the
   * calculation's display name.
   *
   * @param projectPath absolute path to the project directory
   * @param existingItems the current items in the project; used for collision checking
   * @param project the owning project (set as the back-reference on the new calculation)
   * @param model the physics model selection chosen by the user
   * @param name the requested calculation name
   * @return the newly created {@link AFFrCalculation}
   * @throws IOException if {@code name} is blank, collides with an existing item, or the directory
   *     or any file cannot be created
   */
  public static AFFrCalculation createCalculation(
      Path projectPath,
      List<? extends ProjectItem> existingItems,
      AFFrProject project,
      AFFrCalculationModel model,
      String name)
      throws IOException {
    if (name.isBlank()) {
      throw new IOException("Calculation name must not be blank.");
    }
    String trimmed = name.trim();
    for (ProjectItem item : existingItems) {
      if (item.name().equalsIgnoreCase(trimmed)) {
        throw new IOException("A calculation named '" + trimmed + "' already exists.");
      }
    }
    Path calDir = projectPath.resolve(trimmed);
    if (Files.exists(calDir)) {
      throw new IOException("'" + trimmed + "' already exists in this project");
    }
    Files.createDirectory(calDir);
    writeCalProperty(calDir, AFFrCalProperty.DEFAULT);
    writeCalModel(calDir, model);
    return new AFFrCalculation(trimmed, calDir, project, AFFrCalProperty.DEFAULT, model);
  }

  // ── Rename ─────────────────────────────────────────────────────────────────

  /**
   * Renames {@code cal}'s directory to {@code newName} inside {@code projectPath}.
   *
   * <p>Validation: {@code newName} must be non-blank and must not collide (case-insensitively) with
   * any existing item name in {@code existingItems}.
   *
   * @param projectPath absolute path to the project directory
   * @param cal the calculation to rename
   * @param newName the new directory name
   * @param existingItems all current items in the project (including {@code cal})
   * @return a new {@link AFFrCalculation} reflecting the new name and path
   * @throws IOException if the name is blank, collides with an existing item, or the directory
   *     cannot be renamed
   */
  public static AFFrCalculation renameCalculation(
      Path projectPath,
      AFFrCalculation cal,
      String newName,
      List<? extends ProjectItem> existingItems)
      throws IOException {
    if (newName.isBlank()) {
      throw new IOException("Calculation name must not be blank.");
    }
    String trimmed = newName.trim();
    for (ProjectItem item : existingItems) {
      if (!item.name().equals(cal.name()) && item.name().equalsIgnoreCase(trimmed)) {
        throw new IOException("A calculation named '" + trimmed + "' already exists.");
      }
    }
    Path oldDir = cal.path();
    Path newDir = projectPath.resolve(trimmed);
    Files.move(oldDir, newDir);
    return new AFFrCalculation(
        trimmed, newDir, cal.getProject(), cal.getProperty(), cal.getModel());
  }

  // ── Copy ───────────────────────────────────────────────────────────────────

  /**
   * Copies the entire calculation directory to a new sequential name ({@code cal_01}, {@code
   * cal_02}, …) inside {@code projectPath}.
   *
   * <p>All files and sub-directories are copied recursively.
   *
   * @param projectPath absolute path to the project directory
   * @param cal the source calculation
   * @param existingItems current items in the project; used to determine the next name
   * @param project the owning project for the new calculation's back-reference
   * @return the newly created {@link AFFrCalculation}
   * @throws IOException if the directory cannot be copied
   */
  public static AFFrCalculation copyCalculation(
      Path projectPath,
      AFFrCalculation cal,
      List<? extends ProjectItem> existingItems,
      AFFrProject project)
      throws IOException {
    String name = nextCalName(existingItems);
    Path destDir = projectPath.resolve(name);
    if (Files.exists(destDir)) {
      throw new IOException("'" + name + "' already exists in this project");
    }
    Path srcDir = cal.path();
    Files.walkFileTree(
        srcDir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Files.createDirectories(destDir.resolve(srcDir.relativize(dir)));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.copy(
                file,
                destDir.resolve(srcDir.relativize(file)),
                StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
    AFFrCalProperty copiedProperty = cal.getProperty().withStatus(CalculationStatus.SETTING);
    writeCalProperty(destDir, copiedProperty);
    return new AFFrCalculation(name, destDir, project, copiedProperty, cal.getModel());
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  /**
   * Deletes the calculation directory and all of its contents recursively.
   *
   * @param cal the calculation to delete
   * @throws IOException if any file or directory cannot be deleted
   */
  public static void deleteCalculation(AFFrCalculation cal) throws IOException {
    Path dir = cal.path();
    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
            if (exc != null) throw exc;
            Files.delete(d);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  // ── Name generation ────────────────────────────────────────────────────────

  /**
   * Returns the next sequential calculation name ({@code cal_01}, {@code cal_02}, …) given the
   * already-existing project items. Items whose names do not match {@code cal_NN} are ignored.
   */
  public static String nextCalName(List<? extends ProjectItem> items) {
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
