package groovyx.javafx.factory
import groovy.util.AbstractFactory
import groovy.util.FactoryBuilderSupport
import javafx.stage.FileChooser

class FileChooserFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (value instanceof FileChooser) {
            return value
        }
        return new FileChooser()
    }

    @Override
    boolean isLeaf() { false }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        FileChooser fc = (FileChooser) parent

        if (child instanceof FileChooser.ExtensionFilter) {
            fc.extensionFilters.add((FileChooser.ExtensionFilter) child)
            return
        }

        // allow nested list of filters too
        if (child instanceof Collection) {
            (child as Collection).each { c ->
                if (c instanceof FileChooser.ExtensionFilter) {
                    fc.extensionFilters.add((FileChooser.ExtensionFilter) c)
                }
            }
            return
        }

        // otherwise ignore or throw (I prefer throw to catch bad DSL)
        throw new IllegalArgumentException("fileChooser only accepts ExtensionFilter children (got ${child?.getClass()?.name})")
    }

    @Override
    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        FileChooser fc = (FileChooser) node

        if (attributes.containsKey('title')) {
            fc.title = attributes.remove('title')?.toString()
        }

        if (attributes.containsKey('initialDirectory')) {
            fc.initialDirectory = (File) attributes.remove('initialDirectory')
        }

        if (attributes.containsKey('extensionFilter')) {
            def ef = attributes.remove('extensionFilter')

            if (ef instanceof FileChooser.ExtensionFilter) {
                fc.extensionFilters.add((FileChooser.ExtensionFilter) ef)
            }
        }

        if (attributes.containsKey('extensionFilters')) {
            def efs = attributes.remove('extensionFilters')

            if (efs instanceof Iterable) {
                efs.each { f ->
                    if (f instanceof FileChooser.ExtensionFilter) {
                        fc.extensionFilters.add((FileChooser.ExtensionFilter) f)
                    }
                }
            }
        }

        return true
    }
}
