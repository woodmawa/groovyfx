package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.*
import spock.lang.Specification

/**
 * DSL-level smoke tests to verify registerControls() is wiring
 * TabPane/SplitPane/Accordion to the specialized factories.
 *
 * These are intentionally shallow: they validate child-routing semantics,
 * not full DSL design/coverage (that's Phase C).
 */
class CSceneGraphBuilderContainersSmokeSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "smoke: listView items + item routing works end-to-end"() {
        when:
        def lv = runFx {
            new SceneGraphBuilder().build {
                listView(items: ['a','b','c'])
            } as ListView
        }

        then:
        lv.items*.toString() == ['a', 'b', 'c']
    }

    def "smoke: choiceBox + comboBox build with items attribute"() {
        when:
        def res = runFx {
            def b = new SceneGraphBuilder()
            def cb1 = b.build { choiceBox(items: ['A','B']) } as javafx.scene.control.ChoiceBox
            def cb2 = b.build { comboBox(items: ['X','Y']) } as javafx.scene.control.ComboBox
            [cb1, cb2]
        }

        then:
        res[0].items*.toString() == ['A','B']
        res[1].items*.toString() == ['X','Y']
    }
}