package groovyx.javafx.components

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class RibbonTab {

    private final StringProperty text = new SimpleStringProperty(this, "text", "")
    private final ObservableList<RibbonGroup> groups = FXCollections.observableArrayList()

    RibbonTab() {}

    RibbonTab(String text) {
        setText(text)
    }

    StringProperty textProperty() { text }
    String getText() { text.get() }
    void setText(String v) { text.set(v) }

    ObservableList<RibbonGroup> getGroups() { groups }

    void addGroup(RibbonGroup group) {
        groups.add(group)
    }

    @Override
    String toString() { "RibbonTab(${getText()})" }
}
