// File: src/test/groovy/groovyx/javafx/phaseb/ChoiceBoxContractSpec.groovy
package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.collections.FXCollections
import javafx.scene.control.ChoiceBox
import spock.lang.Specification

class ChoiceBoxContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "choiceBox accepts items: ObservableList and exposes them via ChoiceBox.items"() {
        when:
        def cb = runFx {
            def items = FXCollections.observableArrayList("A", "B", "C")
            new SceneGraphBuilder().choiceBox(items: items)
        }

        then:
        cb instanceof ChoiceBox
        cb.items*.toString() == ["A", "B", "C"]
        cb.items.size() == 3
    }

    def "choiceBox selectionModel selects by index and updates selectedItem"() {
        when:
        def cb = runFx {
            def items = FXCollections.observableArrayList("A", "B", "C")
            def box = new SceneGraphBuilder().choiceBox(items: items) as ChoiceBox
            box.selectionModel.select(2)
            box
        }

        then:
        cb.selectionModel.selectedIndex == 2
        cb.selectionModel.selectedItem == "C"
        cb.value == "C"
    }
}
