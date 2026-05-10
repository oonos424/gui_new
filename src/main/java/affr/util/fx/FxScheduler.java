package affr.util.fx;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;

/**
 * Thread-dispatch seam for ViewModels that need to perform IO off the JavaFX Application Thread and
 * then publish results back onto it.
 *
 * <p>This interface lets a ViewModel express the two-step pattern "do this on a background thread,
 * then update observable state on the FX thread" without depending on {@link Platform} or {@link
 * javafx.concurrent.Task} directly. Tests substitute {@link #synchronous()} so async logic can be
 * exercised without TestFX or a running FX toolkit.
 */
public interface FxScheduler {

  /** Submits {@code task} to a background worker. Must not block the FX thread. */
  void runIo(Runnable task);

  /**
   * Marshals {@code task} onto the JavaFX Application Thread. If already on that thread,
   * implementations are free to run the task inline.
   */
  void runUi(Runnable task);

  /**
   * Returns the production scheduler: a daemon-thread executor for IO and {@link Platform#runLater}
   * for UI dispatch.
   */
  static FxScheduler defaultInstance() {
    return DefaultHolder.INSTANCE;
  }

  /**
   * Returns a synchronous scheduler that runs both {@code runIo} and {@code runUi} inline on the
   * calling thread. Intended for unit tests; never use in production because IO will block the FX
   * thread.
   */
  static FxScheduler synchronous() {
    return new FxScheduler() {
      @Override
      public void runIo(Runnable task) {
        task.run();
      }

      @Override
      public void runUi(Runnable task) {
        task.run();
      }
    };
  }

  /**
   * Lazy holder so the executor is only created when {@link #defaultInstance()} is first called.
   */
  final class DefaultHolder {
    private static final FxScheduler INSTANCE = createDefault();

    private DefaultHolder() {}

    private static FxScheduler createDefault() {
      AtomicInteger counter = new AtomicInteger();
      Executor io =
          Executors.newCachedThreadPool(
              r -> {
                Thread t = new Thread(r, "affr-fx-io-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
              });
      return new FxScheduler() {
        @Override
        public void runIo(Runnable task) {
          io.execute(task);
        }

        @Override
        public void runUi(Runnable task) {
          if (Platform.isFxApplicationThread()) {
            task.run();
          } else {
            Platform.runLater(task);
          }
        }
      };
    }
  }
}
