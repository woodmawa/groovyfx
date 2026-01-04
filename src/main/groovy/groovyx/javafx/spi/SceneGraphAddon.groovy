package groovyx.javafx.spi

import groovyx.javafx.SceneGraphBuilder

/**
 * Implementations can register factories / variables / methods into a SceneGraphBuilder.
 * Discovered via Java ServiceLoader.
 */
interface SceneGraphAddon {
    /**
     * Called once per builder instance during initialization.
     */
    void apply(SceneGraphBuilder builder)
}