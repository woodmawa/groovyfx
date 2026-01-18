package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovyx.javafx.components.IconButton

/**
 * Factory for IconButton DSL node.
 *
 * Supports:
 *   iconButton(text:"Copy", name:"copy")
 *   iconButton(text:"Copy", glyph:"copy")
 *   iconButton(text:"Copy", iconName:"copy")
 *   iconButton(action: someAction)
 */
class IconButtonFactory extends AbstractNodeFactory {

    IconButtonFactory() {
        super(IconButton)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attrs) {

        // map DSL aliases to IconButton.iconName
        def icon = attrs.remove('name') ?: attrs.remove('glyph')
        if (icon && !attrs.containsKey('iconName')) {
            attrs.iconName = icon
        }

        return super.newInstance(builder, name, value, attrs)
    }
}
