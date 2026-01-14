package groovyx.javafx.module

import javafx.scene.Node

/**
 * Convenience helpers for working with UIModules.
 * Keeps the core UIModule interface minimal and SAM-friendly.
 */
final class Modules {
    private Modules() {}

    static Node build(UIModule module) {
        module.build([:])
    }

    static Node build(UIModule module, Map ctx) {
        module.build(ctx ?: [:])
    }
}
