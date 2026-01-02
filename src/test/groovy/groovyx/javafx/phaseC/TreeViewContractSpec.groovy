package groovyx.javafx.phaseC
// File: src/test/groovy/groovyx/javafx/phaseC/TreeViewContractSpec.groovy

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.test.FxTestSupport
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import spock.lang.Specification

class TreeViewContractSpec extends Specification {

    def setupSpec() {
        FxTestSupport.ensureStarted()
    }

    private static <T> T runFx(Closure<T> c) {
        FxTestSupport.runFx(c)
    }

    def "treeView accepts root: TreeItem and sets TreeView.root"() {
        when:
        def tv = runFx {
            def root = new TreeItem("root")
            new SceneGraphBuilder().treeView(root: root)
        }

        then:
        tv instanceof TreeView
        tv.root != null
        tv.root.value == "root"
    }

    def "treeItem DSL builds parent/child hierarchy"() {
        when:
        TreeItem root = runFx {
            new SceneGraphBuilder().build {
                treeItem("root") {
                    treeItem("child1")
                    treeItem("child2")
                }
            } as TreeItem
        }

        then:
        root.value == "root"
        root.children.size() == 2
        root.children[0].value == "child1"
        root.children[1].value == "child2"
    }

    def "treeView can be built with a root TreeItem as a nested child"() {
        when:
        def tv = runFx {
            new SceneGraphBuilder().build {
                treeView {
                    treeItem("root") {
                        treeItem("child")
                    }
                }
            } as TreeView
        }

        then:
        tv.root != null
        tv.root.value == "root"
        tv.root.children.size() == 1
        tv.root.children[0].value == "child"
    }
}
