package groovyx.javafx.module.blueprint

import groovy.util.logging.Slf4j
import javafx.scene.Node

@Slf4j
class BlueprintRecordingBuilder {

    /** map: "vbox" -> javafx.scene.layout.VBox, etc */
    final Map<String, Class<? extends Node>> typeIndex

    BlueprintRecordingBuilder(Map<String, Class<? extends Node>> typeIndex) {
        this.typeIndex = typeIndex
    }

    Blueprint build(@DelegatesTo(value=BlueprintRecordingBuilder, strategy=Closure.DELEGATE_FIRST) Closure cl) {
        def result = cl.rehydrate(this, this, this)
        result.resolveStrategy = Closure.DELEGATE_FIRST
        def root = result.call()
        if (!(root instanceof Blueprint)) {
            throw new IllegalStateException("blueprint { } must return a Blueprint root, got: ${root?.getClass()?.name}")
        }
        return (Blueprint) root
    }

    def methodMissing(String name, Object argsObj) {
        def args = (argsObj instanceof Object[]) ? (Object[]) argsObj : [argsObj] as Object[]
        Map attrs = [:]
        Closure body = null

        if (args.length == 1 && args[0] instanceof Closure) {
            body = (Closure) args[0]
        } else if (args.length == 1 && args[0] instanceof Map) {
            attrs = (Map) args[0]
        } else if (args.length == 2 && args[0] instanceof Map && args[1] instanceof Closure) {
            attrs = (Map) args[0]
            body = (Closure) args[1]
        } else if (args.length != 0) {
            throw new IllegalArgumentException("Unsupported args for '${name}': ${args*.getClass()}")
        }

        Class<? extends Node> type = typeIndex[name]
        if (type == null) {
            throw new IllegalStateException("Unknown node type '${name}' in blueprint DSL")
        }

        Map<String,Object> props = [:]
        Map<String,String> hooks = [:]

        attrs.each { k, v ->
            String key = String.valueOf(k)
            if (key.startsWith("on") && v instanceof String) {
                hooks[key] = (String) v
            } else {
                props[key] = v
            }
        }

        List children = []
        if (body != null) {
            def childCollector = new ChildCollector()
            def b = body.rehydrate(childCollector, childCollector, childCollector)
            b.resolveStrategy = Closure.DELEGATE_FIRST
            b.call()
            children.addAll(childCollector.children)
        }

        return new Blueprint(
                type: type,
                props: props,
                hooks: hooks,
                children: children
        )
    }

    private class ChildCollector {
        final List children = []

        def methodMissing(String name, Object args) {
            def bp = BlueprintRecordingBuilder.this.methodMissing(name, args)
            children << bp
            return bp
        }
    }
}
