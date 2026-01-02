package groovyx.javafx.phaseb

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.Accordion
import javafx.scene.control.ButtonBar
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TitledPane
import javafx.scene.control.Label
import javafx.scene.control.ToolBar
import spock.lang.Specification

/**
 * DSL-level smoke tests to verify registerControls() is wiring
 * TabPane/SplitPane/Accordion to the specialized factories.
 *
 * These are intentionally shallow: they validate child-routing semantics,
 * not full DSL design/coverage (that's Phase C).
 */
class B5SceneGraphBuilderContainersSmokeSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "DSL smoke: tabPane nests tab nodes into TabPane.tabs"() {
        given:
        TabPane pane

        when:
        runFx {
            def b = new SceneGraphBuilder()
            pane = b.build {
                tabPane {
                    tab('A')
                    tab('B')
                }
            } as TabPane
        }

        then:
        pane instanceof TabPane
        pane.tabs*.text == ['A', 'B']
        pane.tabs.every { it instanceof Tab }
    }

    def "DSL smoke: splitPane nests Node children into SplitPane.items"() {
        given:
        SplitPane sp

        when:
        runFx {
            def b = new SceneGraphBuilder()
            sp = b.build {
                splitPane {
                    label('L')
                    label('R')
                }
            } as SplitPane
        }

        then:
        sp instanceof SplitPane
        sp.items.size() == 2
        sp.items[0] instanceof Label
        sp.items[1] instanceof Label
        ((Label) sp.items[0]).text == 'L'
        ((Label) sp.items[1]).text == 'R'
    }

    def "DSL smoke: accordion nests titledPane nodes into Accordion.panes"() {
        given:
        Accordion acc

        when:
        runFx {
            def b = new SceneGraphBuilder()
            acc = b.build {
                accordion {
                    // keep it minimal: Phase B is about correct child routing (panes list),
                    // not about titledPane content DSL shape.
                    titledPane('A')
                    titledPane('B')
                }
            } as Accordion
        }

        then:
        acc instanceof Accordion
        acc.panes*.text == ['A', 'B']
        acc.panes.every { it instanceof TitledPane }
    }

    def "DSL smoke: toolBar nests Node children into ToolBar.items"() {
        given:
        ToolBar tb

        when:
        FxTestSupport.runFx {
            def b = new SceneGraphBuilder()
            tb = b.build {
                toolBar {
                    button("A")
                    button("B")
                }
            } as ToolBar
        }

        then:
        tb.items.size() == 2
        tb.items[0].class.name.contains("Button")
        tb.items[1].class.name.contains("Button")
    }

    def "DSL smoke: scrollPane sets single Node child as ScrollPane.content"() {
        given:
        ScrollPane sp

        when:
        FxTestSupport.runFx {
            def b = new SceneGraphBuilder()
            sp = b.build {
                scrollPane {
                    label("Hello")
                }
            } as ScrollPane
        }

        then:
        sp.content != null
        sp.content.class.name.contains("Label")
    }

    def "DSL smoke: scrollPane rejects multiple Node children"() {
        when:
        FxTestSupport.runFx {
            def b = new SceneGraphBuilder()
            b.build {
                scrollPane {
                    label("One")
                    label("Two") // should fail
                }
            }
        }

        then:
        thrown(IllegalStateException)
    }

    def "DSL smoke: buttonBar nests Node children into ButtonBar.buttons"() {
        given:
        ButtonBar bb

        when:
        FxTestSupport.runFx {
            def b = new SceneGraphBuilder()
            bb = b.build {
                buttonBar {
                    button("A")
                    button("B")
                }
            } as ButtonBar
        }

        then:
        bb.buttons.size() == 2
        bb.buttons*.text == ["A", "B"]
    }

    def "DSL reveal: titledPane direct child sets content (current behavior)"() {
        given:
        TitledPane tp

        when:
        FxTestSupport.runFx {
            def b = new SceneGraphBuilder()
            tp = b.build {
                titledPane("A") {
                    label("X")
                }
            } as TitledPane
        }

        then:
        // This assertion tells us whether ControlFactory currently wires content or not.
        tp.content != null
        tp.text == "A"
    }

    def "DSL reveal: titledPane direct child becomes TitledPane.content (current behavior)"() {
        given:
        TitledPane tp


        when:
        FxTestSupport.runFx {
            def b = new SceneGraphBuilder()
            tp = b.build {
                titledPane("A") {
                    label("X")
                }
            } as TitledPane
        }

        then:
        tp.text == "A"
        tp.content != null
        tp.content.class.name.contains("Label")
    }

}