package groovyx.javafx.components.action

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty

/**
 * Non-UI model class.
 * Not a JavaFX Node and does not require a SceneGraphBuilder factory.
 */

class Action {
    String id
    String text
    String icon
    Closure handler
    BooleanProperty enabled = new SimpleBooleanProperty(true)
    BooleanProperty selected = new SimpleBooleanProperty(false)

    void fire() {
        if (enabled.get()) handler?.call()
    }
}