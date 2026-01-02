package groovyx.javafx.phaseb

import groovyx.javafx.factory.AccordionFactory
import javafx.application.Platform
import javafx.scene.control.Accordion
import javafx.scene.control.TitledPane
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AccordionFactoryContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureFxStarted()
    }

    def "AccordionFactory adds TitledPane children to Accordion.panes"() {
        given:
        def factory = new AccordionFactory()
        def parent = new Accordion()
        def a = new TitledPane("A", null)
        def b = new TitledPane("B", null)

        when:
        FxTestSupport.runOnFxAndWait {
            factory.setChild(null, parent, a)
            factory.setChild(null, parent, b)
        }

        then:
        parent.panes.size() == 2
        parent.panes[0].is(a)
        parent.panes[1].is(b)
        parent.panes*.text == ["A", "B"]
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