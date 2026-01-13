package demo.components

import groovyx.javafx.GroovyFX
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {
    stage(title: "ToggleSwitch Demo", width: 520, height: 260, visible: true) {
        scene {
            vbox(spacing: 14, padding: new Insets(16), alignment: Pos.CENTER) {

                label("ToggleSwitch")

                def sw = toggleSwitch(selected: true)

                // ✅ FIX: BooleanProperty doesn't support asString("format")
                label(text: "", id: "stateLabel").with { lbl ->
                    lbl.textProperty().bind(Bindings.format("Selected: %s", sw.selectedProperty()))
                }

                hbox(spacing: 10, alignment: Pos.CENTER) {
                    button("On",  onAction: { sw.selected = true })
                    button("Off", onAction: { sw.selected = false })
                }
            }
        }
    }
}
