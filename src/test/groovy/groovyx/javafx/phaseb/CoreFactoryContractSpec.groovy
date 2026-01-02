package groovyx.javafx.phaseb

import groovyx.javafx.SceneGraphBuilder
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ScrollPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import groovyx.javafx.test.FxTestSupport

class CoreFactoryContractSpec extends Specification {

    @Shared
    static boolean fxStarted = false

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "container adds Node children to public children list"() {
        when:
        def root = runFx {
            new SceneGraphBuilder().vbox {
                button(text: 'A')
                button(text: 'B')
            }
        }

        then:
        root instanceof VBox
        root.children.size() == 2
        root.children*.text == ['A', 'B']
    }

    def "Group supports children insertion safely"() {
        when:
        def group = runFx {
            new SceneGraphBuilder().group {
                button(text: 'X')
            }
        }

        then:
        group instanceof Group
        group.children.size() == 1
        group.children[0] instanceof Button
    }

    def "ScrollPane routes nested Node to content"() {
        when:
        def scroll = runFx {
            new SceneGraphBuilder().scrollPane {
                button(text: 'Inside')
            }
        }

        then:
        scroll instanceof ScrollPane
        scroll.content instanceof Button
        scroll.content.text == 'Inside'
    }

    def "ChoiceBox items attribute is coerced to ObservableList"() {
        when:
        def choice = runFx {
            new SceneGraphBuilder().choiceBox(items: ['one', 'two', 'three'])
        }

        then:
        choice instanceof ChoiceBox
        choice.items.size() == 3
        choice.items[1] == 'two'
    }

    def "onAction closure is wired as EventHandler"() {
        given:
        boolean fired = false

        when:
        def button = runFx {
            new SceneGraphBuilder().button(
                    text: 'Fire',
                    onAction: { fired = true }
            )
        }

        and:
        runFx {
            button.fire()
            null
        }

        then:
        fired
    }

    def "GridPane constraint attributes are consumed and applied"() {
        when:
        def grid = runFx {
            new SceneGraphBuilder().gridPane {
                button(text: 'C', row: 1, col: 2)
            }
        }

        then:
        grid instanceof GridPane
        grid.children.size() == 1

        def child = grid.children[0]
        GridPane.getRowIndex(child) == 1
        GridPane.getColumnIndex(child) == 2
    }
}
