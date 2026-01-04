package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import javafx.scene.Node

/**
 * Adapts a "component class" to a builder node.
 *
 * Supported component styles:
 *  1) static Node build(FactoryBuilderSupport builder, Map attrs, Closure body)
 *  2) static Node build(Map attrs, Closure body)
 *  3) new <T extends Node>() then optionally configure with:
 *        - instance.configure(Map attrs, Closure body)
 *        - or Groovy property assignment from attrs
 *
 * This factory returns a Node and allows nested children (not a leaf).
 */
class ComponentClassFactory extends AbstractFXBeanFactory {

    final Class<?> componentClass

    ComponentClassFactory(Class<?> componentClass) {
        // We don’t know the concrete type up front; use Node as a safe base.
        super(Node)
        this.componentClass = componentClass
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // We must not mutate the original map in surprising ways
        Map attrs = (attributes != null) ? new LinkedHashMap(attributes) : [:]

        // The "value" can be used as shorthand content; pass through if you want.
        // You can choose to interpret it (e.g., as text) but for now we ignore.

        Closure body = null
        // Body is handled by FactoryBuilderSupport via onHandleNodeAttributes / setChild, etc.
        // But for component classes, we want access to the body closure. The simplest approach:
        // users pass it as 'body:' attribute OR you use builder's mechanism to capture it.
        //
        // In GroovyFX scripts, body closure is the node body; we’ll capture it via builder context:
        // builder.current may have the closure; however that’s internal.
        // So: we support passing body via attribute `__body` injected by a custom registerComponent helper.
        if (attrs.containsKey('__body')) {
            body = (Closure) attrs.remove('__body')
        }

        // 1) Prefer static build(builder, attrs, body)
        def m = componentClass.metaClass.getStaticMetaMethod('build', FactoryBuilderSupport, Map, Closure)
        if (m != null) {
            def node = m.invoke(componentClass, builder, attrs, body) as Object
            return assertNode(node, componentClass)
        }

        // 2) static build(attrs, body)
        m = componentClass.metaClass.getStaticMetaMethod('build', Map, Closure)
        if (m != null) {
            def node = m.invoke(componentClass, attrs, body) as Object
            return assertNode(node, componentClass)
        }

        // 3) instance style: new + optional configure(attrs, body) + apply remaining attrs
        def instance = componentClass.getDeclaredConstructor().newInstance()
        if (!(instance instanceof Node)) {
            throw new IllegalArgumentException(
                    "${componentClass.name} must return/extend javafx.scene.Node (or provide a static build(..) that returns Node)"
            )
        }

        // instance.configure(Map, Closure)
        def im = instance.metaClass.getMetaMethod('configure', Map, Closure)
        if (im != null) {
            im.invoke(instance, attrs, body)
        } else {
            // Apply properties from attrs where possible
            attrs.each { k, v ->
                try {
                    instance."$k" = v
                } catch (MissingPropertyException ignored) {
                    // ignore unknown properties, or throw if you want strictness
                }
            }
            // If they provided a body, run it with builder semantics if they want:
            if (body != null) {
                // Run closure with delegate = instance so they can do:
                // acmePanel { spacing = 10 }
                def c = body.rehydrate(instance, body.owner, body.thisObject)
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
            }
        }

        return instance
    }

    @Override
    boolean isLeaf() { false }

    private static Node assertNode(Object o, Class<?> type) {
        if (!(o instanceof Node)) {
            throw new IllegalStateException("${type.name} build(..) must return a javafx.scene.Node but got: ${o?.class?.name}")
        }
        return (Node) o
    }
}