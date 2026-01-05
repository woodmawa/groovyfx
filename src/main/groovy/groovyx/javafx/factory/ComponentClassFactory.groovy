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
    private Closure bodyClosure

    ComponentClassFactory(Class<?> componentClass) {
        // We don’t know the concrete type up front; use Node as a safe base.
        super(Node)
        this.componentClass = componentClass
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // We must not mutate the original map in surprising ways
        Map attrs = (attributes != null) ? new LinkedHashMap(attributes) : [:]

        // Body is handled via isHandlesNodeChildren / onNodeChildren.
        // But for static build style, we might need it NOW.
        // Unfortunately builder calls newInstance BEFORE onNodeChildren.
        // So we support Style A1 (static build) but we have to accept that 
        // if it uses the body, the body must be processed via setChild 
        // OR we must find a way to get the body earlier.
        
        // Actually, if Style A1 returns a Node that is a container, 
        // builder will naturally call setChild for everything in the closure.
        
        // If the user wants Style A1 as documented: 
        // static Node build(b, attrs, body)
        // and they want to call body.call(), they have to realize body 
        // is the closure block.

        // 1) Prefer static build(builder, attrs, body)
        def m = componentClass.metaClass.getStaticMetaMethod('build', FactoryBuilderSupport, Map, Closure)
        if (m != null) {
            // We pass null for body here because we don't have it yet.
            // BUT we return the node.
            def node = m.invoke(componentClass, builder, attrs, null) as Object
            // Remove attributes handled by build if possible, but build is opaque.
            // Documentation says build owns the block.
            attributes?.clear() 
            return assertNode(node, componentClass)
        }

        // 2) static build(attrs, body)
        m = componentClass.metaClass.getStaticMetaMethod('build', Map, Closure)
        if (m != null) {
            def node = m.invoke(componentClass, attrs, null) as Object
            attributes?.clear()
            return assertNode(node, componentClass)
        }

        // 3) instance style: new + optional configure(attrs, body) + apply remaining attrs
        def instance = componentClass.getDeclaredConstructor().newInstance()
        if (!(instance instanceof Node)) {
            throw new IllegalArgumentException(
                    "${componentClass.name} must return/extend javafx.scene.Node (or provide a static build(..) that returns Node)"
            )
        }

        // Apply remaining attributes via standard FXHelper (called by super.onHandleNodeAttributes later)
        return instance
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof Node && child instanceof Node) {
             if (parent instanceof javafx.scene.layout.Pane) {
                 parent.children.add(child)
             } else if (parent instanceof javafx.scene.Group) {
                 parent.children.add(child)
             }
        } else {
            super.setChild(builder, parent, child)
        }
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