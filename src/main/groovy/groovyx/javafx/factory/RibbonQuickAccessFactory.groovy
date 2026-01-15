package groovyx.javafx.factory

import groovyx.javafx.components.RibbonQuickAccessBar
import javafx.scene.Node

class RibbonQuickAccessFactory extends AbstractFXBeanFactory {
    RibbonQuickAccessFactory() { super(RibbonQuickAccessBar) }

    @Override
    void setChild(Object parent, Object child) {
        if (child instanceof Node) {
            parent.items.add(child)
        }
    }
}
