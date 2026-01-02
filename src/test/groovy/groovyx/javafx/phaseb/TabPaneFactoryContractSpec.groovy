package groovyx.javafx.phaseb

import groovyx.javafx.factory.TabPaneFactory
import javafx.application.Platform
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TabPaneFactoryContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureFxStarted()
    }

    def "TabPaneFactory adds Tab children to TabPane.tabs"() {
        given:
        def factory = new TabPaneFactory()
        def parent = new TabPane()
        def child = new Tab('A')

        when:
        FxTestSupport.runOnFxAndWait {
            factory.setChild(null, parent, child)
        }

        then:
        parent.tabs.size() == 1
        parent.tabs[0].is(child)
        parent.tabs[0].text == 'A'
    }

    /**
     * Minimal JavaFX startup/run helper for tests.
     * If you already have a shared helper in the project, feel free to replace usages with yours.
     */
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