package groovyx.javafx.phaseb

import groovyx.javafx.factory.SplitPaneFactory
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SplitPaneFactoryContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureFxStarted()
    }

    def "SplitPaneFactory adds Node children to SplitPane.items"() {
        given:
        def factory = new SplitPaneFactory()
        def parent = new SplitPane()
        def left = new Label("L")
        def right = new Label("R")

        when:
        FxTestSupport.runOnFxAndWait {
            factory.setChild(null, parent, left)
            factory.setChild(null, parent, right)
        }

        then:
        parent.items.size() == 2
        parent.items[0].is(left)
        parent.items[1].is(right)
    }

    private static final class FxTestSupport {
        private static volatile boolean started = false

        static void ensureFxStarted() {
            if (started) return
            synchronized (FxTestSupport) {
                if (started) return
                try {
                    def latch = new CountDownLatch(1)
                    Platform.startup { latch.countDown() }
                    assert latch.await(10, TimeUnit.SECONDS)
                } catch (IllegalStateException ignored) {
                    // Toolkit already started
                }
                started = true
            }
        }

        static void runOnFxAndWait(Closure<?> work) {
            if (Platform.isFxApplicationThread()) {
                work.call()
                return
            }
            def latch = new CountDownLatch(1)
            Platform.runLater {
                try {
                    work.call()
                } finally {
                    latch.countDown()
                }
            }
            assert latch.await(10, TimeUnit.SECONDS)
        }
    }
}