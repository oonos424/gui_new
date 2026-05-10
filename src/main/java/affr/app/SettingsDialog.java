package affr.app;

import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Modal settings dialog that lets the user configure application-level preferences.
 *
 * <p>Currently exposes two installation-path settings: the GUI installation directory (from which
 * the tutorial set is derived) and the solver installation directory. When the user confirms with
 * OK both values are written to {@link UserPreferences} and persisted immediately.
 */
public final class SettingsDialog {

  private SettingsDialog() {}

  /**
   * Opens the settings dialog modally over {@code owner} and, if the user clicks OK, saves any
   * changes to {@code prefs}.
   */
  public static void show(Stage owner, UserPreferences prefs) {
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.initOwner(owner);
    dialog.setTitle(I18n.get("settings.title"));
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    TextField guiPathField =
        makePathField(prefs.guiInstallPath(), "settings.guiInstallPath.placeholder");
    Button guiBrowseButton =
        makeBrowseButton(owner, guiPathField, "settings.guiInstallPath.chooserTitle");
    HBox guiPathRow = makePathRow(guiPathField, guiBrowseButton);

    TextField solverPathField =
        makePathField(prefs.solverInstallPath(), "settings.solverInstallPath.placeholder");
    Button solverBrowseButton =
        makeBrowseButton(owner, solverPathField, "settings.solverInstallPath.chooserTitle");
    HBox solverPathRow = makePathRow(solverPathField, solverBrowseButton);

    GridPane grid = new GridPane();
    grid.setHgap(8);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 24, 8, 24));

    ColumnConstraints labelCol = new ColumnConstraints();
    ColumnConstraints fieldCol = new ColumnConstraints();
    fieldCol.setHgrow(Priority.ALWAYS);
    fieldCol.setMinWidth(360);
    grid.getColumnConstraints().addAll(labelCol, fieldCol);

    grid.add(new Label(I18n.get("settings.guiInstallPath")), 0, 0);
    grid.add(guiPathRow, 1, 0);
    grid.add(new Label(I18n.get("settings.solverInstallPath")), 0, 1);
    grid.add(solverPathRow, 1, 1);

    dialog.getDialogPane().setContent(grid);
    dialog.getDialogPane().setPrefWidth(600);

    dialog
        .showAndWait()
        .ifPresent(
            result -> {
              if (result == ButtonType.OK) {
                prefs.setGuiInstallPath(parsePath(guiPathField.getText()));
                prefs.setSolverInstallPath(parsePath(solverPathField.getText()));
                prefs.save();
              }
            });
  }

  private static TextField makePathField(@Nullable Path current, String placeholderKey) {
    TextField field = new TextField(current != null ? current.toString() : "");
    field.setPromptText(I18n.get(placeholderKey));
    HBox.setHgrow(field, Priority.ALWAYS);
    return field;
  }

  private static Button makeBrowseButton(Stage owner, TextField field, String chooserTitleKey) {
    Button button = new Button(I18n.get("settings.browse"));
    button.setOnAction(
        e -> {
          DirectoryChooser chooser = new DirectoryChooser();
          chooser.setTitle(I18n.get(chooserTitleKey));
          String current = field.getText().trim();
          if (!current.isEmpty()) {
            try {
              Path p = Path.of(current);
              if (Files.isDirectory(p)) {
                chooser.setInitialDirectory(p.toFile());
              }
            } catch (InvalidPathException ignored) {
            }
          }
          File chosen = chooser.showDialog(owner);
          if (chosen != null) {
            field.setText(chosen.getAbsolutePath());
          }
        });
    return button;
  }

  private static HBox makePathRow(TextField field, Button browse) {
    HBox row = new HBox(6, field, browse);
    HBox.setHgrow(field, Priority.ALWAYS);
    return row;
  }

  /** Returns {@code null} if {@code text} is blank or not a valid path. */
  static @Nullable Path parsePath(String text) {
    String trimmed = text.trim();
    if (trimmed.isEmpty()) return null;
    try {
      return Path.of(trimmed);
    } catch (InvalidPathException e) {
      return null;
    }
  }
}
