package demo.components

import groovyx.javafx.GroovyFX
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {
    stage(title: "ResponsivePane Demo", width: 760, height: 420, visible: true) {
        scene {
            vbox(spacing: 12, padding: new Insets(16), alignment: Pos.TOP_CENTER) {

                label("Resize the window to see ResponsivePane adapt (if it has breakpoints).")

                responsivePane {
                    // Put a few “cards” inside as children
                    card {
                        cardHeader { label("Panel A") }
                        cardBody { label("Content A") }
                    }
                    card {
                        cardHeader { label("Panel B") }
                        cardBody { label("Content B") }
                    }
                    card {
                        cardHeader { label("Panel C") }
                        cardBody { label("Content C") }
                    }
                }
            }
        }
    }
}
