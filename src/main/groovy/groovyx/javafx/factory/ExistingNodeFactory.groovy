package groovyx.javafx.factory

import groovy.transform.CompileStatic
import groovy.util.AbstractFactory
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.layout.Pane

@CompileStatic
class ExistingNodeFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // support: node(existingNode)
        if (value instanceof Node) {
            return (Node) value
        }

        // support: node(node: existingNode)
        def n = attributes.remove('node')
        if (n instanceof Node) {
            return (Node) n
        }

        throw new IllegalArgumentException("node(...) requires a JavaFX Node (value or 'node:' attribute)")
    }

    @Override
    void setParent(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof Parent && child instanceof Node) {
            ((Parent) parent).childrenUnmodifiable // touch ok (forces type)
            // Parent doesn't expose mutable children directly, but most Parents used here are Panes/Groups.
        }
        super.setParent(builder, parent, child)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // For panes/groups, ContainerFactory usually handles. But we’ll be explicit:
        if (parent instanceof Pane && child instanceof Node) {
            ((Pane) parent).children.add((Node) child)
            return
        }
        if (parent instanceof Group && child instanceof Node) {
            ((Group) parent).children.add((Node) child)
            return
        }
        // fallback: let normal mechanisms try
        super.setChild(builder, parent, child)
    }

    @Override
    boolean isLeaf() { true }
}
