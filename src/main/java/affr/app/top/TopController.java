package affr.app.top;

import affr.util.i18n.I18n;
import affr.util.prefs.UserPreferences;
import affr.fx.viewmodel.top.TopCategory;
import affr.fx.viewmodel.top.TopViewModel;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.StackPane;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * View controller for the Project Browser screen.
 *
 * <p>This class owns only widgets and the bindings that connect them to its {@link TopViewModel}.
 * It contains no domain data and no application services — those live in (or behind) the ViewModel.
 *
 * <p>Lifecycle: FXML loading injects the widgets, then {@link #initialize()} runs (rendering rules
 * only), then the application calls {@link #init(TopViewModel, UserPreferences)} to wire bindings.
 */
public final class TopController {

  // -------------------------------------------------------------------------
  // FXML-injected widgets (non-null after FXMLLoader.load(); null before)
  // -------------------------------------------------------------------------

  @FXML private @Nullable ListView<TopCategory> categoryList;
  @FXML private @Nullable StackPane viewerPane;
  @FXML private @Nullable RadioMenuItem langEnItem;
  @FXML private @Nullable RadioMenuItem langJaItem;

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
                  {
                    hoverProperty()
                        .addListener(
                            (obs, old, hovered) -> {
                              if (!isSelected()) {
                                setStyle(hovered ? "-fx-background-color: #eaf4ff;" : "");
                              }
                            });
                    selectedProperty()
                        .addListener(
                            (obs, old, selected) -> {
                              if (selected || !isHover()) {
                                setStyle("");
                              }
                            });
                  }

                  @Override
                  protected void updateItem(@Nullable TopCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(item != null && !empty ? I18n.get(item.messageKey()) : null);
                  }
                });
  }

  // -------------------------------------------------------------------------
  // Public API — bind widgets to the supplied ViewModel
  // -------------------------------------------------------------------------

  /**
   * Wires all bindings between widgets and the ViewModel, and sets up the language-selector menu.
   *
   * @param viewModel the ViewModel for this screen
   * @param prefs user preferences used to persist the selected language
   */
  public void init(TopViewModel viewModel, UserPreferences prefs) {
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

    // ── Language menu ────────────────────────────────────────────────────
    syncLanguageMenu(I18n.getLocale());

    requireLangEnItem()
        .setOnAction(
            e -> {
              I18n.setLocale(Locale.ENGLISH);
              prefs.setLocale(Locale.ENGLISH);
              prefs.save();
            });

    requireLangJaItem()
        .setOnAction(
            e -> {
              I18n.setLocale(Locale.JAPANESE);
              prefs.setLocale(Locale.JAPANESE);
              prefs.save();
            });

    // ── Refresh widgets when locale changes ──────────────────────────────
    I18n.bundleProperty()
        .addListener(
            (obs, old, bundle) -> {
              requireCategoryList().refresh();
              renderViewer(requireViewModel().getSelectedCategory());
            });
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  /**
   * Renders a placeholder for the selected category. Each branch will be replaced by a real
   * sub-view (file browser, running calculations, tutorials) loaded from its own FXML/controller.
   */
  private void renderViewer(TopCategory category) {
    Label placeholder = new Label(I18n.get(category.messageKey()));
    placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #888;");
    requireViewerPane().getChildren().setAll(placeholder);
  }

  /** Marks the radio item that matches {@code locale} as selected. */
  private void syncLanguageMenu(Locale locale) {
    boolean isJa = Locale.JAPANESE.getLanguage().equals(locale.getLanguage());
    requireLangJaItem().setSelected(isJa);
    requireLangEnItem().setSelected(!isJa);
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

  private RadioMenuItem requireLangEnItem() {
    RadioMenuItem item = langEnItem;
    if (item == null) {
      throw new IllegalStateException("langEnItem not injected by FXMLLoader");
    }
    return item;
  }

  private RadioMenuItem requireLangJaItem() {
    RadioMenuItem item = langJaItem;
    if (item == null) {
      throw new IllegalStateException("langJaItem not injected by FXMLLoader");
    }
    return item;
  }

  private TopViewModel requireViewModel() {
    TopViewModel vm = viewModel;
    if (vm == null) {
      throw new IllegalStateException("init() has not been called");
    }
    return vm;
  }
}
