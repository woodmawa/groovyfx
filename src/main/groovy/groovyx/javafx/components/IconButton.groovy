package groovyx.javafx.components

import groovyx.javafx.components.action.Action
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.Button

/**
 * A Button with a vector Icon (using groovyx.javafx.components.Icon).
 *
 * Generic component:
 * - Can be used standalone with iconName/text.
 * - Or bound to an Action (id/text/icon/enabled/handler).
 *
 * NOTE: This is a JavaFX Node (Control) and can have a builder factory.
 */
class IconButton extends Button {

    // Optional Action binding
    final ObjectProperty<Action> actionProperty = new SimpleObjectProperty<>(null)

    // Icon configuration (for standalone usage)
    String iconName
    Double iconSize

    IconButton() {
        super()
        initialize()
    }

    IconButton(String text, String iconName = null) {
        super(text)
        this.iconName = iconName
        initialize()
    }

    void setAction(Action a) { actionProperty.set(a) }
    Action getAction() { actionProperty.get() }

    private void initialize() {

        // keep graphic in sync with iconName/iconSize/action
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

        // if action is set, bind UI to it
        actionProperty.addListener ({ ObservableValue, Action oldA, Action newA ->

            // clear previous binds
            disableProperty().unbind()

            if (newA != null) {

                if (!text) text = newA.text ?: text

                if (!iconName) iconName = newA.icon

                setOnAction { newA.fire() }

                if (newA.enabled != null) {
                    disableProperty().bind(newA.enabled.not())
                }
            }

            rebuildGraphic()
        } as ChangeListener<Action>)

        // initial graphic
        rebuildGraphic()
    }
}
