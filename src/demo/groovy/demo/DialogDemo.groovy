package demo

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType

import static groovyx.javafx.GroovyFX.start

start {
    stage(title: "Dialog Demo", width: 420, height: 220) {
        scene (fill: GROOVYBLUE){
            vbox(spacing: 12, padding: 12) {
                label("Click to show Alert. showAndWait() should work cleanly on FX thread.")

                button("Show confirmation") {
                    onAction { e ->
                        def alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Proceed with operation?",
                                ButtonType.OK, ButtonType.CANCEL)

                        alert.title = "Confirm"
                        alert.headerText = "GroovyFX + JavaFX 25 Dialog"

                        def result = alert.showAndWait()
                        println("Dialog result: " + (result.isPresent() ? result.get() : "<none>"))
                    }
                }
            }
        }
    }.show()
}