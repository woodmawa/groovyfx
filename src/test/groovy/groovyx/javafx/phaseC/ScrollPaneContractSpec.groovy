// File: src/test/groovy/groovyx/javafx/phaseb/ScrollPaneContractSpec.groovy
package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import spock.lang.Specification

class ScrollPaneContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "scrollPane sets single Node child as ScrollPane.content"() {
        when:
        def sp = runFx {
            new SceneGraphBuilder().build {
                scrollPane {
                    label("Hello")
                }
            } as ScrollPane
        }

        then:
        sp.content instanceof Label
        (sp.content as Label).text == "Hello"
    }

    def "scrollPane supports content via content attribute as Node"() {
        when:
        def sp = runFx {
            new SceneGraphBuilder().scrollPane(content: new Label("X")) as ScrollPane
        }

        then:
        sp.content instanceof Label
        (sp.content as Label).text == "X"
    }

    def "scrollPane rejects multiple Node children (single-content contract)"() {
        when:
        runFx {
            new SceneGraphBuilder().build {
                scrollPane {
                    label("One")
                    label("Two")
                }
            }
        }

        then:
        thrown(IllegalStateException)
    }
}
