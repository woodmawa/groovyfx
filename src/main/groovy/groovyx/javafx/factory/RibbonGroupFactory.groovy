package groovyx.javafx.factory

import groovyx.javafx.components.RibbonGroup
import javafx.scene.Node

class RibbonGroupFactory extends AbstractFXBeanFactory {
    RibbonGroupFactory() { super(RibbonGroup) }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (value instanceof CharSequence) return new RibbonGroup(value.toString())
        return super.newInstance(builder, name, value, attributes)
    }

    @Override
    void setChild(Object parent, Object child) {
        if (child instanceof Node) parent.add(child)
    }
}
