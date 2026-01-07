package groovyx.javafx.factory

import javafx.scene.Node
import javafx.scene.control.ToolBar

/**
 * ToolBar is a Control, but it holds content in `items` (ObservableList<Node>),
 * not in a Parent children list.
 */
class ToolBarFactory extends AbstractFXBeanFactory {

    ToolBarFactory() {
        super(ToolBar)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Node) {
            ((ToolBar) parent).items.add((Node) child)
            return
        }
        Object.setChild(builder, parent, child)
    }
}
