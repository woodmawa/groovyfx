package demo.components.carousel

import groovy.util.Factory
import groovy.util.FactoryBuilderSupport
import groovyx.javafx.factory.AbstractFXBeanFactory
import javafx.scene.Node

class SimpleCarouselFactory extends AbstractFXBeanFactory{
    @Override boolean isLeaf() { false }

    SimpleCarouselFactory () {
        super(SimpleCarousel, false)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof SimpleCarousel && child instanceof Node) {
            parent.items.add((Node) child)
            parent.refresh()
        }
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        if (node instanceof SimpleCarousel) node.refresh()
    }
}
