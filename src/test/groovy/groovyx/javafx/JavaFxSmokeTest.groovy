package groovyx.javafx

import spock.lang.Specification
import javafx.scene.Group
import javafx.scene.Scene

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal JavaFX smoke test:
 * - Starts JavaFX toolkit (if needed)
 * - Runs on FX thread
 * - Constructs a Scene
 * - Exits cleanly (no hanging CI)
 */
class JavaFxSmokeTest extends Specification {

    def "JavaFX toolkit starts and can build a Scene"() {
        given: "a latch to prove the FX thread ran"
        def latch = new CountDownLatch(1)
        def thrownOnFx = new AtomicReference<Throwable>(null)

        when: "we run simple JavaFX code on the FX thread"
        GroovyFX.runOnFxThread {
            try {
                def scene = new Scene(new Group(), 10, 10)
                assert scene != null
            } catch (Throwable t) {
                thrownOnFx.set(t)
            } finally {
                latch.countDown()
            }
        }

        then: "the FX code ran and did not throw"
        latch.await(10, TimeUnit.SECONDS)
        thrownOnFx.get() == null

        cleanup:
        // Ensure the platform can exit (prevents leaked threads in some environments)
        try {
            //don't call .exit() in tests
            //Platform.exit()
        } catch (Throwable ignored) {
            // safe: platform may already be stopped
        }
    }
}
