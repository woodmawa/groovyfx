package groovyx.javafx.factory

import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import spock.lang.Specification

class ButtonBarFactoryContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    def "ButtonBarFactory adds Node children to ButtonBar.buttons"() {
        given:
        def factory = new ButtonBarFactory()
        def parent = new ButtonBar()
        def a = new Button("A")
        def b = new Button("B")

        when:
        FxTestSupport.runFx {
            factory.setChild(null, parent, a)
            factory.setChild(null, parent, b)
        }

        then:
        parent.buttons.size() == 2
        parent.buttons[0].is(a)
        parent.buttons[1].is(b)
    }
}
