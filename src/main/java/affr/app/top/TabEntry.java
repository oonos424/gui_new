package affr.app.top;

import javafx.scene.Node;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a single entry in the workspace tab bar.
 *
 * <p>{@code onClose} is {@code null} for the pinned primary tab (Browser), which has no × button.
 * Secondary tabs (Input Editor, Mesh Viewer, etc.) supply a non-null {@code onClose} callback that
 * is invoked both when the × button is clicked and when the Back/Home actions inside the tab
 * content close the tab.
 */
record TabEntry(String title, Node content, @Nullable Runnable onClose) {}
