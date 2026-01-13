package groovyx.javafx.components

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.layout.StackPane

/**
 * A single slide. For v1 this is just a StackPane container.
 * Later we can add per-slide metadata (title/id/transition/etc).
 */
class CarouselSlide extends StackPane {
    private final StringProperty titleProperty = new SimpleStringProperty(this, "title", null)

    String getTitle() { titleProperty.get() }
    void setTitle(String t) { titleProperty.set(t) }
    StringProperty titleProperty() { titleProperty }
}