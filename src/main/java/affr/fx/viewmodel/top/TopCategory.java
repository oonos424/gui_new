package affr.fx.viewmodel.top;

/**
 * The three top-level navigation modes available in the Project Browser.
 *
 * <p>Owned by the ViewModel layer: the View consumes the {@link #label()} only for cell rendering.
 * The label string here is a temporary stand-in; presentation strings will move to a resource
 * bundle in a later iteration.
 */
public enum TopCategory {
  FILE("ファイル"),
  RUNNING("実行中の計算"),
  TUTORIALS("チュートリアル");

  private final String label;

  TopCategory(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
