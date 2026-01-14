package groovyx.javafx.appsupport

import groovy.transform.CompileStatic
import javafx.application.Platform

import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
final class FxToolkit {
    private static final AtomicBoolean started = new AtomicBoolean(false)

    private FxToolkit() {}

    static void ensureStarted() {
        if (started.compareAndSet(false, true)) {
            Platform.startup { /* no-op */ }
        }
    }
}
