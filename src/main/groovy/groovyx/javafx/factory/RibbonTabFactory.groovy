package groovyx.javafx.factory

import groovyx.javafx.components.RibbonTab
import groovyx.javafx.components.RibbonGroup

class RibbonTabFactory extends AbstractFXBeanFactory {
    RibbonTabFactory() { super(RibbonTab) }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (value instanceof CharSequence) return new RibbonTab(value.toString())
        return super.newInstance(builder, name, value, attributes)
    }

    @Override
    void setChild(Object parent, Object child) {
        if (child instanceof RibbonGroup) parent.addGroup(child)
    }
}
