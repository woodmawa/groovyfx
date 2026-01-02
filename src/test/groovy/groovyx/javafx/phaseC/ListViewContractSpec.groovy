// File: src/test/groovy/groovyx/javafx/phasec/ListViewContractSpec.groovy
package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import spock.lang.Specification

class ListViewContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "listView accepts items: List and populates ListView.items"() {
        when:
        def lv = runFx {
            new SceneGraphBuilder().listView(items: ["A", "B", "C"])
        }

        then:
        lv instanceof ListView
        lv.items*.toString() == ["A", "B", "C"]
        lv.items.size() == 3
    }

    def "listView selectionModel works with provided items"() {
        when:
        def lv = runFx {
            def view = new SceneGraphBuilder().listView(items: ["A", "B", "C"]) as ListView
            view.selectionModel.select(1)
            view
        }

        then:
        lv.selectionModel.selectedIndex == 1
        lv.selectionModel.selectedItem == "B"
    }

    def "listView supports cellFactory closure and produces ListCell instances"() {
        when:
        def lv = runFx {
            new SceneGraphBuilder().listView(items: ["A"]) {
                // Keep it minimal: just prove the factory is set and can produce a ListCell.
                cellFactory {
                    new ListCell()
                }
            } as ListView
        }

        then:
        lv.cellFactory != null

        when:
        def cell = runFx {
            // cellFactory is a Callback; in Groovy we can call it like a closure.
            lv.cellFactory.call(lv)
        }

        then:
        cell instanceof ListCell
    }
}
