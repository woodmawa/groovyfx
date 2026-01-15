package groovyx.javafx.components

import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Skin

class RibbonGroup extends Control {

    private final StringProperty text = new SimpleStringProperty(this, "text", "")
    private final ObservableList<Node> items = FXCollections.observableArrayList()

    private final BooleanProperty dialogLauncherVisible =
            new SimpleBooleanProperty(this, "dialogLauncherVisible", false)

    private final ObjectProperty<EventHandler<ActionEvent>> onDialogLauncher =
            new SimpleObjectProperty<>(this, "onDialogLauncher")

    RibbonGroup() {
        getStyleClass().add("ribbon-group")
    }

    RibbonGroup(String title) {
        this()
        setText(title)
    }

    StringProperty textProperty() { text }
    String getText() { text.get() }
    void setText(String v) { text.set(v) }

    ObservableList<Node> getItems() { items }
    void add(Node node) { items.add(node) }

    BooleanProperty dialogLauncherVisibleProperty() { dialogLauncherVisible }
    boolean isDialogLauncherVisible() { dialogLauncherVisible.get() }
    void setDialogLauncherVisible(boolean v) { dialogLauncherVisible.set(v) }

    ObjectProperty<EventHandler<ActionEvent>> onDialogLauncherProperty() { onDialogLauncher }
    EventHandler<ActionEvent> getOnDialogLauncher() { onDialogLauncher.get() }
    void setOnDialogLauncher(EventHandler<ActionEvent> h) { onDialogLauncher.set(h) }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new groovyx.javafx.components.skin.RibbonGroupSkin(this)
    }
}
