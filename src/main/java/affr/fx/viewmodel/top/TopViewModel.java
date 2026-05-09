package affr.fx.viewmodel.top;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Project Browser (Top) screen.
 *
 * <p>Exposes the available top-level categories and the currently active selection as observable
 * properties. Holds no widget references and no FXML knowledge: any number of views may bind to
 * this ViewModel without coupling to one another.
 *
 * <p>Persistence of the active category across sessions (per {@code PRD/10/01_top_navigator.md})
 * will be handled by an external listener that observes {@link #selectedCategoryProperty()}; that
 * concern is intentionally outside this class.
 */
public final class TopViewModel {

  private final ObservableList<TopCategory> categories =
      FXCollections.observableArrayList(TopCategory.values());

  private final ObjectProperty<TopCategory> selectedCategory =
      new SimpleObjectProperty<>(TopCategory.FILE);

  public ObservableList<TopCategory> getCategories() {
    return categories;
  }

  public ObjectProperty<TopCategory> selectedCategoryProperty() {
    return selectedCategory;
  }

  public TopCategory getSelectedCategory() {
    return selectedCategory.get();
  }

  public void setSelectedCategory(TopCategory category) {
    selectedCategory.set(category);
  }
}
