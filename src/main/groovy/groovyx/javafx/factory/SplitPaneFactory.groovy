package groovyx.javafx.factory

import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.SplitPane

class SplitPaneFactory extends AbstractFXBeanFactory {

    SplitPaneFactory() {
        super(SplitPane)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Node) {
            parent.items.add((Node) child)
            return
        }
        Object.setChild(builder, parent, child)
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // If you support dividerPositions attribute, apply here after items exist:
        // e.g. attributes consumed earlier into builder context, then:
        // node.setDividerPositions(...)
        Object.onNodeCompleted(builder, parent, node)
    }
}
