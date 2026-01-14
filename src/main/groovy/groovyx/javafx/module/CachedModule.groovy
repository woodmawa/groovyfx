package groovyx.javafx.module

import groovy.transform.CompileStatic
import groovyx.javafx.SceneGraphBuilder
import javafx.scene.Node

/**
 * A "cached" module: stores the DSL closure and replays it on demand to
 * produce a fresh Node graph each time.
 *
 * Safe default: does NOT reuse Node instances.
 */
@CompileStatic
final class CachedModule implements UIModule {
    private final Closure<?> dsl

    CachedModule(Closure<?> dsl) {
        assert dsl != null
        this.dsl = dsl
    }

    @Override
    Node build(Map ctx) {
        Map safeCtx = (ctx ?: Collections.emptyMap())

        // New builder per instantiation (fresh node graph)
        def builder = new SceneGraphBuilder()

        // Rehydrate so delegate/owner are the builder (DSL-friendly)
        Closure<?> c = dsl.rehydrate(builder, builder, builder)
        c.resolveStrategy = Closure.DELEGATE_FIRST

        def result = (c.maximumNumberOfParameters == 1) ? c.call(safeCtx) : c.call()

        if (!(result instanceof Node)) {
            throw new IllegalStateException(
                    "Module DSL must return a JavaFX Node, but returned: " +
                            (result == null ? "null" : result.getClass().name)
            )
        }
        return (Node) result
    }
}
