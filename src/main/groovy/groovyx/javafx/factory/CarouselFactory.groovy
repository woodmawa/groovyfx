package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovyx.javafx.components.Carousel
import groovyx.javafx.components.CarouselSlide

class CarouselFactory extends AbstractFXBeanFactory {

    CarouselFactory() {
        super(Carousel, false)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof Carousel && child instanceof CarouselSlide) {
            parent.slides.add((CarouselSlide) child)
        }
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        if (node instanceof Carousel) {
            // clamp index after all slides are collected
            node.goTo(node.index)
        }
    }
}
