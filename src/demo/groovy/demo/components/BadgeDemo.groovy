package demo.components

import groovyx.javafx.GroovyFX
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {
    stage(title: "Badge Demo", width: 520, height: 240, visible: true) {
        scene {
            vbox(spacing: 12, padding: new Insets(16), alignment: Pos.CENTER) {
                label("Badges")

                hbox(spacing: 10, alignment: Pos.CENTER) {
                    badge(text: "Default")
                    badge(text: "New", style: "-fx-background-color: #2d6cdf; -fx-text-fill: white;")
                    badge(text: "Hot", style: "-fx-background-color: #e55353; -fx-text-fill: white;")
                    badge(text: "OK", style: "-fx-background-color: #2fb344; -fx-text-fill: white;")
                }
            }
        }
    }
}
