package groovyx.javafx.factory

import groovyx.javafx.components.Ribbon
import groovyx.javafx.components.RibbonTab

class RibbonFactory extends AbstractFXBeanFactory {
    RibbonFactory() { super(Ribbon) }

    @Override
    void setChild(Object parent, Object child) {
        if (child instanceof RibbonTab) {
            parent.tabs.add(child)
        }
    }
}
