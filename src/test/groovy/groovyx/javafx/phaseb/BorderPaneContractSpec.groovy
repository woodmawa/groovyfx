package groovyx.javafx.phaseb

import groovyx.javafx.SceneGraphBuilder
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import groovyx.javafx.test.FxTestSupport


class BorderPaneContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "borderPane routes region nodes via top/left/right/bottom/center wrappers"() {
        when:
        def pane = runFx {
            new SceneGraphBuilder().borderPane {
                top { label("T") }
                left { label("L") }
                right { label("R") }
                bottom { label("B") }
                center { button("C") }
            }
        }

        then:
        pane instanceof BorderPane
        pane.top instanceof Label
        pane.left instanceof Label
        pane.right instanceof Label
        pane.bottom instanceof Label
        pane.center instanceof Button
        ((Label) pane.top).text == "T"
        ((Label) pane.left).text == "L"
        ((Label) pane.right).text == "R"
        ((Label) pane.bottom).text == "B"
        ((Button) pane.center).text == "C"
    }

    def "borderPane defaults a nested Node to center for compatibility"() {
        when:
        def pane = runFx {
            new SceneGraphBuilder().borderPane {
                button("Only")
            }
        }

        then:
        pane instanceof BorderPane
        pane.center instanceof Button
        ((Button) pane.center).text == "Only"
    }
}
