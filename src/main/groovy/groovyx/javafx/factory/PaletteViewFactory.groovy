package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovy.util.AbstractFactory
import groovyx.javafx.components.palette.PaletteView

class PaletteViewFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        def view = new PaletteView()

        // allow: paletteView(model: x, onItemActivated: {...})
        if (attributes?.containsKey("model")) {
            view.model = attributes.remove("model")
        }
        if (attributes?.containsKey("onItemActivated")) {
            view.onItemActivated = attributes.remove("onItemActivated")
        }
        return view
    }

    @Override
    boolean isLeaf() { true }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // nothing
    }
}
