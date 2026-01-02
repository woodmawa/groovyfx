package groovyx.javafx.phaseb

import groovyx.javafx.SceneGraphBuilder
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import groovyx.javafx.test.FxTestSupport


class GridPaneContractSpec extends Specification {


    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "gridPane applies row/col, spans, margin, and hgrow/vgrow constraints"() {
        when:
        def grid = runFx {
            new SceneGraphBuilder().gridPane {
                // Exercise shorthand + aliases:
                // col -> columnIndex
                // colspan/rowspan -> columnSpan/rowSpan
                // margin list -> Insets
                // hgrow string, vgrow boolean
                button(text: "A",
                        row: 1,
                        col: 2,
                        colspan: 3,
                        rowspan: 2,
                        margin: [1, 2, 3, 4],
                        hgrow: "always",
                        vgrow: true)
            }
        }

        then:
        grid instanceof GridPane
        grid.children.size() == 1

        def node = grid.children[0]
        node instanceof Button
        ((Button) node).text == "A"

        GridPane.getRowIndex(node) == 1
        GridPane.getColumnIndex(node) == 2

        GridPane.getColumnSpan(node) == 3
        GridPane.getRowSpan(node) == 2

        GridPane.getMargin(node) == new Insets(1, 2, 3, 4)

        GridPane.getHgrow(node) == Priority.ALWAYS
        GridPane.getVgrow(node) == Priority.ALWAYS
    }

    def "gridPane supports span shorthand as [colSpan, rowSpan]"() {
        when:
        def grid = runFx {
            new SceneGraphBuilder().gridPane {
                button(text: "B", row: 0, col: 0, span: [2, 3])
            }
        }

        then:
        grid instanceof GridPane
        grid.children.size() == 1

        def node = grid.children[0]
        node instanceof Button
        ((Button) node).text == "B"

        GridPane.getColumnSpan(node) == 2
        GridPane.getRowSpan(node) == 3
    }

    def "gridPane treats cols/rows as span aliases (not indices)"() {
        when:
        def grid = runFx {
            new SceneGraphBuilder().gridPane {
                button(text: "C", row: 0, col: 0, cols: 2, rows: 4)
            }
        }

        then:
        grid instanceof GridPane
        grid.children.size() == 1

        def node = grid.children[0]
        node instanceof Button
        ((Button) node).text == "C"

        GridPane.getColumnIndex(node) in [null, 0]
        GridPane.getRowIndex(node) in [null, 0]
        GridPane.getColumnSpan(node) == 2
        GridPane.getRowSpan(node) == 4
    }
}
