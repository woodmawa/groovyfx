package groovyx.javafx.phaseb

import groovyx.javafx.SceneGraphBuilder
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import groovyx.javafx.test.FxTestSupport

class StageSceneStylesheetContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "stage->scene builds headless and wires root"() {
        when:
        Stage stage = runFx {
            new SceneGraphBuilder().stage("Test") {
                scene {
                    vbox {
                        label("Hello")
                    }
                }
            }
        }

        then:
        stage instanceof Stage
        stage.title == "Test"
        stage.scene instanceof Scene
        stage.scene.root instanceof VBox
        ((VBox) stage.scene.root).children.size() == 1
        ((VBox) stage.scene.root).children[0] instanceof Label
        ((Label) ((VBox) stage.scene.root).children[0]).text == "Hello"
    }

    def "scene accepts stylesheet child and records it"() {
        when:
        def scene = runFx {
            new SceneGraphBuilder().scene {
                // use a simple css ref (no need for a real file for this test)
                stylesheet("app.css")
                vbox { label("X") }
            }
        }

        then:
        scene.stylesheets.contains("app.css")
    }

    def "stage accepts stylesheet and applies it to its scene"() {
        when:
        Stage stage = runFx {
            new SceneGraphBuilder().stage {
                stylesheet("theme.css")
                scene {
                    vbox { label("Y") }
                }
            }
        }

        then:
        stage.scene != null
        stage.scene.stylesheets.contains("theme.css")
    }
}
