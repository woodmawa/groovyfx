// File: src/test/groovy/groovyx/javafx/phaseb/ComboBoxContractSpec.groovy
package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import spock.lang.Specification

class ComboBoxContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "comboBox accepts items: ObservableList and exposes them via ComboBox.items"() {
        when:
        def cb = runFx {
            def items = FXCollections.observableArrayList("A", "B", "C")
            new SceneGraphBuilder().comboBox(items: items)
        }

        then:
        cb instanceof ComboBox
        cb.items*.toString() == ["A", "B", "C"]
        cb.items.size() == 3
    }

    def "comboBox selectionModel selects by index and updates value"() {
        when:
        def cb = runFx {
            def items = FXCollections.observableArrayList("A", "B", "C")
            def box = new SceneGraphBuilder().comboBox(items: items) as ComboBox
            box.selectionModel.select(1)
            box
        }

        then:
        cb.selectionModel.selectedIndex == 1
        cb.selectionModel.selectedItem == "B"
        cb.value == "B"
    }

    def "comboBox supports editable flag and can set value directly"() {
        when:
        def cb = runFx {
            def items = FXCollections.observableArrayList("A", "B", "C")
            def box = new SceneGraphBuilder().comboBox(items: items, editable: true) as ComboBox
            box.value = "C"
            box
        }

        then:
        cb.editable
        cb.value == "C"
    }
}
