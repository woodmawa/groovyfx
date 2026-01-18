package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovyx.javafx.components.IconToggleButton

/**
 * Factory for IconToggleButton DSL node.
 *
 * Supports:
 *   iconToggleButton(text:"Create", name:"pencil")
 *   iconToggleButton(text:"Create", glyph:"pencil")
 *   iconToggleButton(text:"Create", iconName:"pencil")
 *   iconToggleButton(action: someAction)
 */
class IconToggleButtonFactory extends AbstractNodeFactory {

    IconToggleButtonFactory() {
        super(IconToggleButton)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attrs) {

        // map DSL aliases to IconToggleButton.iconName
        def icon = attrs.remove('name') ?: attrs.remove('glyph')
        if (icon && !attrs.containsKey('iconName')) {
            attrs.iconName = icon
        }

        return super.newInstance(builder, name, value, attrs)
    }
}
