// File: src/test/groovy/groovyx/javafx/phaseb/TableViewContractSpec.groovy
package groovyx.javafx.phaseC

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.collections.FXCollections
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import spock.lang.Specification

class TableViewContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "tableView accepts items: ObservableList and exposes them via TableView.items"() {
        when:
        def tv = runFx {
            def items = FXCollections.observableArrayList("A", "B", "C")
            new SceneGraphBuilder().tableView(items: items)
        }

        then:
        tv instanceof TableView
        tv.items*.toString() == ["A", "B", "C"]
        tv.items.size() == 3
    }

    def "tableView DSL adds tableColumn children to TableView.columns"() {
        when:
        def tv = runFx {
            new SceneGraphBuilder().build {
                tableView {
                    tableColumn("ColA")
                    tableColumn("ColB")
                }
            } as TableView
        }

        then:
        tv.columns.size() == 2
        tv.columns[0] instanceof TableColumn
        tv.columns[1] instanceof TableColumn
        tv.columns*.text == ["ColA", "ColB"]
    }

    def "tableColumn DSL supports nested columns (column groups)"() {
        when:
        def tv = runFx {
            new SceneGraphBuilder().build {
                tableView {
                    tableColumn("Group") {
                        tableColumn("Child1")
                        tableColumn("Child2")
                    }
                }
            } as TableView
        }

        then:
        tv.columns.size() == 1
        tv.columns[0].text == "Group"
        tv.columns[0].columns.size() == 2
        tv.columns[0].columns*.text == ["Child1", "Child2"]
    }
}