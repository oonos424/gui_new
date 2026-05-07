package affr.app.top;

import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * View controller for the Project Browser screen.
 *
 * <p>This class owns only widgets and the bindings that connect them to its {@link TopViewModel}.
 * It contains no domain data and no application services — those live in (or behind) the ViewModel.
 *
 * <p>Lifecycle: FXML loading injects the widgets, then {@link #initialize()} runs (rendering rules
 * only), then the application calls {@link #init(TopViewModel)} to wire bindings.
 */
public final class TopController {

  // -------------------------------------------------------------------------
  // FXML-injected widgets (non-null after FXMLLoader.load(); null before)
  // -------------------------------------------------------------------------

  @FXML private @Nullable ListView<TopCategory> categoryList;
  @FXML private @Nullable StackPane viewerPane;

  // -------------------------------------------------------------------------
  // ViewModel reference (single non-FXML field; set by init())
  // -------------------------------------------------------------------------

  private @Nullable TopViewModel viewModel;

  // -------------------------------------------------------------------------
  // FXML lifecycle — rendering rules only, no data
  // -------------------------------------------------------------------------

  @FXML
  private void initialize() {
    requireCategoryList()
        .setCellFactory(
            lv ->
                new ListCell<>() {
                  @Override
                  protected void updateItem(@Nullable TopCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item != null && !empty ? item.label() : null);
                  }
                });
  }

  // -------------------------------------------------------------------------
  // Public API — bind widgets to the supplied ViewModel
  // -------------------------------------------------------------------------

  public void init(TopViewModel viewModel) {
    this.viewModel = viewModel;

    ListView<TopCategory> list = requireCategoryList();
    list.setItems(viewModel.getCategories());

    // ── ListView selection → ViewModel ──────────────────────────────────
    list.getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, old, selected) -> {
              if (selected != null) {
                viewModel.setSelectedCategory(selected);
              }
            });

    // ── ViewModel selection → ListView + viewer area ────────────────────
    // The selection-model select() is idempotent, so writing the same value
    // back from VM does not re-fire the listener above.
    viewModel
        .selectedCategoryProperty()
        .addListener(
            (obs, old, selected) -> {
              list.getSelectionModel().select(selected);
              renderViewer(selected);
            });

    // Initial sync: reflect the VM's starting state in the widgets.
    list.getSelectionModel().select(viewModel.getSelectedCategory());
    renderViewer(viewModel.getSelectedCategory());
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Renders a placeholder for the selected category. Each branch will be replaced by a real
   * sub-view (file browser, running calculations, tutorials) loaded from its own FXML/controller.
   */
  private void renderViewer(TopCategory category) {
    Label placeholder = new Label(category.label());
    placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #888;");
    requireViewerPane().getChildren().setAll(placeholder);
  }

  private ListView<TopCategory> requireCategoryList() {
    ListView<TopCategory> list = categoryList;
    if (list == null) {
      throw new IllegalStateException("categoryList not injected by FXMLLoader");
    }
    return list;
  }

  private StackPane requireViewerPane() {
    StackPane pane = viewerPane;
    if (pane == null) {
      throw new IllegalStateException("viewerPane not injected by FXMLLoader");
    }
    return pane;
  }
}
