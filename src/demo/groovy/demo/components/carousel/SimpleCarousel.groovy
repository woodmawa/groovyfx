package demo.components.carousel

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.layout.StackPane

/**
 * Minimal carousel: shows exactly one Node from 'items' at a time.
 * No Skin; keeps demo simple and proves SPI + builder integration.
 */
class SimpleCarousel extends StackPane {
    final ObservableList<Node> items = FXCollections.observableArrayList()

    final IntegerProperty indexProperty = new SimpleIntegerProperty(this, "index", 0)
    final BooleanProperty wrapProperty  = new SimpleBooleanProperty(this, "wrap", true)

    SimpleCarousel() {
        indexProperty.addListener { _, _, _ -> refresh() }

        items.addListener((ListChangeListener<Node>) { _ ->
            if (indexProperty.get() >= items.size()) {
                indexProperty.set(Math.max(items.size() - 1, 0))
            } else {
                refresh()
            }
        })

        refresh()
    }

    int getIndex() { indexProperty.get() }
    void setIndex(int i) { indexProperty.set(i) }

    boolean isWrap() { wrapProperty.get() }
    void setWrap(boolean b) { wrapProperty.set(b) }

    void next() {
        if (items.isEmpty()) return
        int i = getIndex() + 1
        if (i >= items.size()) setIndex(isWrap() ? 0 : items.size() - 1)
        else setIndex(i)
    }

    void prev() {
        if (items.isEmpty()) return
        int i = getIndex() - 1
        if (i < 0) setIndex(isWrap() ? items.size() - 1 : 0)
        else setIndex(i)
    }

    void refresh() {
        children.clear()
        if (items.isEmpty()) return

        int i = getIndex()
        if (i < 0) i = 0
        if (i >= items.size()) i = items.size() - 1
        if (i != getIndex()) setIndex(i)

        Node n = items.get(i)
        if (n != null) children.add(n)
    }
}
