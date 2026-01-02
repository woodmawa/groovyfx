package groovyx.javafx.test

import javafx.application.Platform

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FxTestSupport {

    private static final AtomicBoolean started = new AtomicBoolean(false)

    static void ensureStarted() {
        if (started.get()) return

        synchronized (FxTestSupport) {
            if (started.get()) return

            CountDownLatch latch = new CountDownLatch(1)
            try {
                Platform.startup {
                    latch.countDown()
                }
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("JavaFX Platform failed to start within timeout")
                }
            } catch (IllegalStateException ex) {
                // JavaFX throws if already started (Toolkit already initialized)
                // or if it was previously exited (Platform.exit has been called).
                def msg = ex.message ?: ""
                if (!(msg.contains("Toolkit already initialized") || msg.contains("Platform.exit has been called"))) {
                    throw ex
                }
            }

            started.set(true)
        }
    }

    static <T> T runFx(Closure<T> c) {
        ensureStarted()

        if (Platform.isFxApplicationThread()) {
            return c.call()
        }

        CountDownLatch latch = new CountDownLatch(1)
        def ref = new java.util.concurrent.atomic.AtomicReference<T>()
        def err = new java.util.concurrent.atomic.AtomicReference<Throwable>()

        Platform.runLater {
            try {
                ref.set((T) c.call())
            } catch (Throwable t) {
                err.set(t)
            } finally {
                latch.countDown()
            }
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for FX thread")
        }
        if (err.get() != null) throw err.get()
        return ref.get()
    }
}
