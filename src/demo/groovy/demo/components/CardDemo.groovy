package demo.components

import groovyx.javafx.GroovyFX
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {
    stage(title: "Card Demo", width: 620, height: 420, visible: true) {
        scene {
            vbox(spacing: 14, padding: new Insets(16), alignment: Pos.TOP_CENTER) {

                card {
                    cardHeader {
                        label("Card Header", style: "-fx-font-size: 18px; -fx-font-weight: bold;")
                    }
                    cardBody {
                        vbox(spacing: 8) {
                            label("This is the card body.")
                            label("Put any nodes here: labels, forms, tables, etc.")
                            button("Action")
                        }
                    }
                    cardFooter {
                        hbox(spacing: 10, alignment: Pos.CENTER_RIGHT) {
                            button("Cancel")
                            button("Save")
                        }
                    }
                }
            }
        }
    }
}
