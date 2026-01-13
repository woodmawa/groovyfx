package demo.components

import groovyx.javafx.GroovyFX
import groovyx.javafx.components.Notification
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.util.Duration

new GroovyFX().start {
    stage(id: "ownerStage", title: "Notification Demo", width: 600, height: 280, visible: true) {
        scene {
            vbox(spacing: 14, padding: new Insets(16), alignment: Pos.CENTER) {

                label("Notification (Toast)")

                button("Show notification") {
                    onAction {
                        Notification.show(ownerStage, "Hello from Notification!", Duration.seconds(3))
                    }
                }

                button("Show longer (5s)") {
                    onAction {
                        Notification.show(ownerStage, "This one stays up longer.", Duration.seconds(5))
                    }
                }
            }
        }
    }
}
