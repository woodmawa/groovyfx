package groovyx.javafx.components

import groovyx.javafx.components.action.Action
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.ToggleButton

/**
 * A ToggleButton with a vector Icon (using groovyx.javafx.components.Icon).
 *
 * Generic component:
 * - Can be used standalone with iconName/text.
 * - Or bound to an Action (id/text/icon/enabled/handler/selected).
 *
 * NOTE: This is a JavaFX Node (Control) and can have a builder factory.
 */
class IconToggleButton extends ToggleButton {

    // Optional Action binding
    final ObjectProperty<Action> actionProperty = new SimpleObjectProperty<>(null)

    // Icon configuration (for standalone usage)
    String iconName
    Double iconSize

    IconToggleButton() {
        super()
        initialize()
    }

    IconToggleButton(String text, String iconName = null) {
        super(text)
        this.iconName = iconName
        initialize()
    }

    void setAction(Action a) { actionProperty.set(a) }
    Action getAction() { actionProperty.get() }

    private void initialize() {

        def rebuildGraphic = {
            def useIcon = (action?.icon ?: iconName)
            if (!useIcon) {
                graphic = null
                return
            }
            def icon = new Icon(iconName: useIcon)
            if (iconSize != null) icon.size = iconSize
            graphic = icon
        }

        actionProperty.addListener ({ ObservableValue obs, oldA, newA ->
            // clear previous binds
            disableProperty().unbind()
            selectedProperty().unbind()

            if (newA != null) {
                // if toggle text not explicitly set, use action text
                if (!text) text = newA.text ?: text

                // icon
                if (!iconName) iconName = newA.icon

                // handler (toggle action fires on click)
                setOnAction { newA.fire() }

                // enabled binding (disable = !enabled)
                if (newA.enabled != null) {
                    disableProperty().bind(newA.enabled.not())
                }

                // selected binding (if action supports it)
                if (newA.metaClass.hasProperty(newA, "selected") && newA.selected != null) {
                    selectedProperty().bindBidirectional(newA.selected)
                }
            }

            rebuildGraphic()
        }  as ChangeListener<Action>)

        rebuildGraphic()
    }
}
