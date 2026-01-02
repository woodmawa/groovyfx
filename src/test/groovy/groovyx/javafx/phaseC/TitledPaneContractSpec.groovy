// File: src/test/groovy/groovyx/javafx/phaseb/TitledPaneContractSpec.groovy
package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import spock.lang.Specification

class TitledPaneContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "titledPane supports text attribute"() {
        when:
        def tp = runFx {
            new SceneGraphBuilder().build {
                titledPane(text: "Hello")
            } as TitledPane
        }

        then:
        tp.text == "Hello"
    }

    def "titledPane supports content via content node wrapper"() {
        when:
        def tp = runFx {
            new SceneGraphBuilder().build {
                titledPane(text: "T") {
                    content {
                        label("Body")
                    }
                }
            } as TitledPane
        }

        then:
        tp.text == "T"
        tp.content instanceof Label
        (tp.content as Label).text == "Body"
    }

    def "titledPane supports graphic via graphic node wrapper"() {
        when:
        def tp = runFx {
            new SceneGraphBuilder().build {
                titledPane(text: "T") {
                    graphic {
                        label("G")
                    }
                }
            } as TitledPane
        } as TitledPane

        then:
        tp.graphic instanceof Label
        (tp.graphic as Label).text == "G"
    }
}
