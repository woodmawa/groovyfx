package groovyx.javafx.module


import javafx.scene.Node

/**
 * A runtime unit of UI that can be instantiated cheaply at runtime.
 *
 * Design rules:
 * - returns a *new* Node graph each time build() is called
 * - accepts only explicit context (no builder internals)
 */
@FunctionalInterface
interface UIModule {
    Node build(Map ctx)
}