package groovyx.javafx.module.blueprint

import groovy.util.logging.Slf4j
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane

@Slf4j
class Blueprint {

    Class<? extends Node> type
    Map<String, Object> props = [:]
    Object children = []
    Map<String, String> hooks = [:]

    Node instantiate(Map ctx) {
        Map safeCtx = (ctx ?: [:])

        Node n = type.getDeclaredConstructor().newInstance()

        // Apply properties (plain values or Closure(ctx)->value)
        props.each { k, v ->
            applyProp(n, String.valueOf(k), v, safeCtx)
        }

        // Apply named hooks (e.g. onAction: "saveDiagram")
        if (hooks) {
            def handlers = safeCtx.handlers
            if (!(handlers instanceof Map)) {
                handlers = [:]
            }

            hooks.each { String propName, String handlerName ->
                if (!hasWritableProperty(n, propName)) {
                    log.warn "instantiate(): Unknown hook property '${propName}' for ${n.getClass().name}; hook '${handlerName}' ignored"
                    return null
                }

                Closure h = resolveHandler(handlers, handlerName)
                if (h == null) {
                    throw new IllegalStateException(
                            "Blueprint hook '${propName}' refers to handler '${handlerName}', " +
                                    "but ctx.handlers did not contain a Closure at that path."
                    )
                }

                Closure wrapper = { Object event ->
                    BlueprintModule.invokeHook(h, event, safeCtx, n)
                }
                n."$propName" = wrapper

                return null
            }
        }

        // Attach children (slot-aware containers first)
        if (children != null) {

            // BorderPane slot attachment (children must be a Map)
            if (n instanceof BorderPane && (children instanceof Map)) {
                Map slotMap = (Map) children
                slotMap.each { k, v ->
                    String slot = String.valueOf(k)
                    Node childNode = asNode(v, safeCtx)
                    switch (slot) {
                        case 'top':    ((BorderPane) n).top = childNode; break
                        case 'bottom': ((BorderPane) n).bottom = childNode; break
                        case 'left':   ((BorderPane) n).left = childNode; break
                        case 'right':  ((BorderPane) n).right = childNode; break
                        case 'center': ((BorderPane) n).center = childNode; break
                        default:
                            log.warn "instantiate(): Unknown BorderPane slot '${slot}' in blueprint for ${type?.name}"
                    }
                }

                // ScrollPane content attachment
            } else if (n instanceof ScrollPane) {
                ScrollPane sp = (ScrollPane) n

                if (children instanceof Map) {
                    Map m = (Map) children
                    if (m.containsKey('content')) {
                        sp.content = asNode(m.content, safeCtx)
                    } else {
                        log.warn "instantiate(): ScrollPane children map expects key 'content' (got: ${m.keySet()})"
                    }
                } else if (children instanceof List) {
                    List list = (List) children
                    if (list.size() == 1) {
                        sp.content = asNode(list[0], safeCtx)
                    } else if (list.size() > 1) {
                        log.warn "instantiate(): ScrollPane expects a single child; got ${list.size()} children"
                    }
                } else {
                    sp.content = asNode(children, safeCtx)
                }

                // Default behavior: Parent.children
            } else if (n instanceof Parent) {
                Parent p = (Parent) n
                if (children instanceof List) {
                    p.children.addAll(((List) children).collect { asNode(it, safeCtx) })
                } else {
                    p.children.add(asNode(children, safeCtx))
                }

            } else {
                log.warn "instantiate(): Blueprint specified children for non-container node ${n.getClass().name}; children ignored"
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

    private Node asNode(Object child, Map safeCtx) {
        if (child == null) return null
        if (child instanceof Blueprint) return ((Blueprint) child).instantiate(safeCtx)
        if (child instanceof Node) return (Node) child
        throw new IllegalArgumentException("Blueprint child must be a Blueprint or Node, got: ${child.getClass().name}")
    }

    private boolean hasWritableProperty(Node n, String name) {
        def mp = n.metaClass.getMetaProperty(name)
        if (mp != null) return true

        // Fallback: look for setter method: setX(...)
        String setter = "set${name.capitalize()}"
        return !n.metaClass.respondsTo(n, setter).isEmpty()
    }

    private void applyProp(Node n, String k, Object v, Map safeCtx) {
        Object value = (v instanceof Closure) ? ((Closure) v).call(safeCtx) : v

        if (!hasWritableProperty(n, k)) {
            log.warn "instantiate(): Unknown prop '${k}' for ${n.getClass().name} (value=${value}); ignored"
            return
        }

        try {
            n."$k" = value
        } catch (Throwable t) {
            log.warn "instantiate(): Failed to set prop '${k}' on ${n.getClass().name} (value=${value}): ${t.class.simpleName}: ${t.message}"
        }
    }
}
