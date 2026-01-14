package groovyx.javafx.appsupport

import javafx.application.Platform

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FxToolkit {

    private static final AtomicBoolean started = new AtomicBoolean(false)

    static void ensureStarted() {
        // fast path
        if (started.get()) return

        // only one thread attempts startup
        if (!started.compareAndSet(false, true)) return

        // If JavaFX is already running (e.g., other tests started it),
        // Platform.startup would throw. We can detect by calling runLater.
        try {
            // This will throw IllegalStateException if toolkit not initialized
            Platform.runLater({ /* noop */ } as Runnable)
            return
        } catch (IllegalStateException ignored) {
            // not started yet, continue to startup
        }

        CountDownLatch latch = new CountDownLatch(1)
        Platform.startup({
            latch.countDown()
        } as Runnable)

        // Avoid hanging tests if something goes wrong
        latch.await(5, TimeUnit.SECONDS)
    }
}
