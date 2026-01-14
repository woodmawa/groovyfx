package groovyx.javafx.module.blueprint

import javafx.scene.Parent
import javafx.scene.Node

class Blueprint {
    Class<? extends Node> type
    Map<String, Object> props = [:]
    List<Blueprint> children = []

    Node instantiate(Map ctx) {
        Node n = type.getDeclaredConstructor().newInstance()

        props.each { k, v ->
            n."$k" = (v instanceof Closure ? v.call(ctx) : v)
        }

        if (n instanceof Parent) {
            children.each { bp ->
                n.children.add(bp.instantiate(ctx))
            }
        }

        return n
    }
}
