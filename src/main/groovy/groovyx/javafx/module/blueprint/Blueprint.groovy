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
                def h = handlers[handlerName]
                if (!(h instanceof Closure)) {
                    throw new IllegalStateException(
                            "Blueprint hook '${propName}' refers to handler '${handlerName}', " +
                                    "but ctx.handlers['${handlerName}'] was not a Closure (was: ${h?.getClass()?.name ?: 'null'})"
                    )
                }
                // JavaFX expects EventHandler, Groovy closure coerces fine
                n."$propName" = (Closure) h
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
}
