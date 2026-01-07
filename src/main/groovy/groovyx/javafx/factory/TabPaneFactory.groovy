package groovyx.javafx.factory

import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane

class TabPaneFactory extends AbstractFXBeanFactory {

    TabPaneFactory() {
        super(TabPane)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {
        return Object.newInstance(builder, name, value, attributes)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Tab) {
            parent.tabs.add(child)
            return
        }

        // Optional convenience:
        // If someone puts a Node directly under tabPane, wrap it in a Tab.
        if (child instanceof Node) {
            def tab = new Tab()
            tab.content = (Node) child
            parent.tabs.add(tab)
            return
        }

        Object.setChild(builder, parent, child)
    }

    // Optional: handle selectedIndex after tabs added (see below)
    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // if you support selectedIndex attribute, apply it here (after children)
        Object.onNodeCompleted(builder, parent, node)
    }
}
