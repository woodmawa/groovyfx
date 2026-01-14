package groovyx.javafx.module.blueprint

import javafx.scene.Node
import javafx.scene.Parent

class Blueprint {

    Class<? extends Node> type
    Map<String, Object> props = [:]
    List<Blueprint> children = []
    Map<String, String> hooks = [:]

    Node instantiate(Map ctx) {
        Map safeCtx = (ctx ?: [:])

        Node n = type.getDeclaredConstructor().newInstance()

        // Apply properties (plain values or Closure(ctx)->value)
        props.each { k, v ->
            n."$k" = (v instanceof Closure ? v.call(safeCtx) : v)
        }

        // Apply named hooks (e.g. onAction: "saveDiagram")
        if (hooks) {
            def handlers = safeCtx.handlers
            if (!(handlers instanceof Map)) {
                handlers = [:]
            }

            hooks.each { String propName, String handlerName ->
                Closure h = resolveHandler(handlers, handlerName)
                if (h == null) {
                    throw new IllegalStateException(
                            "Blueprint hook '${propName}' refers to handler '${handlerName}', " +
                                    "but ctx.handlers did not contain a Closure at that path."
                    )
                }

                // A1: standardize hook invocation context: (event, ctx, node)
                // Wrap so JavaFX always calls us with the event, then we adapt to handler arity.
                Closure wrapper = { Object event ->
                    BlueprintModule.invokeHook(h, event, safeCtx, n)
                }

                n."$propName" = wrapper
            }
        }

        // Attach children if this is a Parent
        if (n instanceof Parent) {
            Parent p = (Parent) n
            children.each { bp ->
                p.children.add(bp.instantiate(safeCtx))
            }
        }

        return n
    }

    private static Closure resolveHandler(Object handlers, String path) {
        if (!(handlers instanceof Map)) return null
        if (!path) return null

        Object cur = handlers
        for (String part : path.split('\\.')) {
            if (!(cur instanceof Map)) return null
            cur = ((Map) cur).get(part)
        }
        return (cur instanceof Closure) ? (Closure) cur : null
    }
}
