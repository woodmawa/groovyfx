package groovyx.javafx.factory

import groovy.util.AbstractFactory
import groovy.util.FactoryBuilderSupport
import javafx.stage.DirectoryChooser

class DirectoryChooserFactory extends AbstractFactory {
    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (value instanceof DirectoryChooser) return value
        return new DirectoryChooser()
    }

    @Override
    boolean isLeaf() { true }

    @Override
    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        DirectoryChooser dc = (DirectoryChooser) node

        if (attributes.containsKey('title')) {
            dc.title = attributes.remove('title')?.toString()
        }
        if (attributes.containsKey('initialDirectory')) {
            dc.initialDirectory = (File) attributes.remove('initialDirectory')
        }
        return true
    }
}

