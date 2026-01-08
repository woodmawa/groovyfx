package groovyx.javafx.factory

import groovy.util.AbstractFactory
import groovy.util.FactoryBuilderSupport
import javafx.event.Event
import javafx.event.EventHandler

class ClosureHandlerFactory extends AbstractFactory {

    private final Class handlerType

    ClosureHandlerFactory(Class handlerType) {
        this.handlerType = handlerType
    }

    @Override
    boolean isLeaf() { false }

    // CRITICAL: without this, FactoryBuilderSupport will NOT call onNodeChildren(...)
    @Override
    boolean isHandlesNodeChildren() { true }

    static class HandlerSpec {
        final String name
        Closure closure
        HandlerSpec(String name) { this.name = name }
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // value will be null for "onFinished { ... }" style calls
        return new HandlerSpec(name?.toString())
    }

    @Override
    boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {
        if (node instanceof HandlerSpec) {
            node.closure = childContent
            return false // don't execute as nested DSL
        }
        return true
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        if (!(node instanceof HandlerSpec)) return

        if (node.closure == null) {
            throw new RuntimeException(
                    "The '${node.name}' node requires a Closure, e.g. ${node.name} { evt -> ... }"
            )
        }

        Closure c = node.closure
        EventHandler<Event> handler = { Event evt ->
            if (c.maximumNumberOfParameters == 0) c.call()
            else c.call(evt)
        }

        FXHelper.setPropertyOrMethod(parent, node.name, handler)
    }
}
