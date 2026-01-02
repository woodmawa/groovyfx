package groovyx.javafx.factory
/**
 * Minimal replacement for groovy.swing.factory.CollectionFactory
 * used by builder-style DSLs to aggregate child nodes into a Collection.
 *
 * Intentionally small: just enough for GroovyFX SceneGraphBuilder.
 */
class CollectionFactory extends AbstractFactory {

    @Override
    boolean isLeaf() { false }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // If caller provides a collection, use it
        if (value instanceof Collection) {
            return value
        }

        // Optional: allow specifying collection implementation via attribute
        // e.g. collection(type: LinkedHashSet) { ... }
        def type = attributes?.remove('type')
        if (type instanceof Class && Collection.isAssignableFrom(type)) {
            return type.getDeclaredConstructor().newInstance()
        }

        // If caller passes an array, convert
        if (value?.getClass()?.isArray()) {
            return value.toList()
        }

        // If caller passes a single element, wrap
        if (value != null) {
            def c = new ArrayList()
            c.add(value)
            return c
        }

        return new ArrayList()
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof Collection) {
            parent.add(child)
        } else {
            super.setChild(builder, parent, child)
        }
    }
}
