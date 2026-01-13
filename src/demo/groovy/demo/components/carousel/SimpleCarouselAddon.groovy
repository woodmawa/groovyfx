package demo.components.carousel

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.spi.SceneGraphAddon

class SimpleCarouselAddon implements SceneGraphAddon {
    @Override
    void apply(SceneGraphBuilder builder) {
        // debug while wiring:
        //println "SimpleCarouselAddon.apply() called"

        builder.registerFactory('simpleCarousel', new SimpleCarouselFactory())
        // If your builder uses map-style registration instead:
        // builder.factories['simpleCarousel'] = new SimpleCarouselFactory()
    }
}
