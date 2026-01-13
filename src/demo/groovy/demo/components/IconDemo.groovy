package demo.components

import groovyx.javafx.GroovyFX
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.paint.Color

new GroovyFX().start {
    stage(title: "Icon Demo", width: 520, height: 260, visible: true) {
        scene {
            vbox(spacing: 14, padding: new Insets(16), alignment: Pos.CENTER) {

                label("Icons")

                hbox(spacing: 18, alignment: Pos.CENTER) {

                    // Standalone icon
                    icon(name: "check", size: 28, fill: Color.BLACK)

                    // Icon next to text
                    hbox(spacing: 8, alignment: Pos.CENTER_LEFT) {
                        icon(name: "info", size: 20, fill: Color.BLACK)
                        label("Info", style: "-fx-font-size: 14px;")
                    }
                }
            }
        }
    }
}
