package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovyx.javafx.components.CarouselSlide
import javafx.scene.Node

class CarouselSlideFactory extends AbstractFXBeanFactory {

    CarouselSlideFactory() {
        super(CarouselSlide, false) // accepts children
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof CarouselSlide && child instanceof Node) {
            parent.children.add((Node) child)
        }
    }
}