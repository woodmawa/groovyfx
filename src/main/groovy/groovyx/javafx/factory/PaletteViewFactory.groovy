package groovyx.javafx.factory

import groovyx.javafx.components.PaletteView
import groovyx.javafx.factory.AbstractNodeFactory
import groovy.util.FactoryBuilderSupport

class PaletteViewFactory extends AbstractNodeFactory {

    PaletteViewFactory() {
        super(PaletteView)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attrs) {
        return super.newInstance(builder, name, value, attrs)
    }
}
