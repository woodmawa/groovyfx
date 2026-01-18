package groovyx.javafx.components.palette

import javafx.scene.control.Button
import javafx.scene.control.TitledPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.VBox

class PaletteView extends VBox {

    PaletteModel model
    Closure onItemActivated   // called with PaletteItem

    PaletteView() {
        spacing = 6
    }

    void setModel(PaletteModel model) {
        this.model = model
        rebuild()
    }

    void rebuild() {
        children.clear()
        if (!model) return

        model.groups.each { PaletteGroup group ->
            children << buildGroup(group)
        }
    }

    protected TitledPane buildGroup(PaletteGroup group) {
        def content = new FlowPane(hgap: 6, vgap: 6)

        group.items.each { PaletteItem item ->
            content.children << buildItemButton(item)
        }

        def tp = new TitledPane(group.label, content)
        tp.expanded = true
        tp.collapsible = true
        return tp
    }

    protected Button buildItemButton(PaletteItem item) {
        def b = new Button(item.label)
        b.disable = !item.enabled
        b.setOnAction {
            if (onItemActivated) onItemActivated.call(item)
        }
        return b
    }
}
