package groovyx.javafx.module.blueprint

import groovy.transform.CompileStatic
import groovyx.javafx.module.UIModule
import javafx.scene.Node

/**
 * UIModule backed by a Blueprint (no DSL execution at runtime).
 */
@CompileStatic
final class BlueprintModule implements UIModule {

    Blueprint blueprint

    BlueprintModule() {} // no-arg ctor enables map-style construction

    BlueprintModule(Blueprint blueprint) {
        this.blueprint = blueprint
    }

    @Override
    Node build(Map ctx) {
        blueprint.instantiate(ctx ?: [:])
    }

    static Object invokeHook(Closure handler, Object event, Map ctx, Node node) {
        if (handler == null) return null

        int n = handler.maximumNumberOfParameters
        if (n <= 0) return handler.call()
        if (n == 1) return handler.call(event)
        if (n == 2) return handler.call(event, ctx)
        return handler.call(event, ctx, node)
    }
}