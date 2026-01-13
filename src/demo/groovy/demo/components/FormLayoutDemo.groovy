package demo.components

import groovyx.javafx.GroovyFX
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {
    stage(title: "FormLayout Demo", width: 720, height: 420, visible: true) {
        scene {
            vbox(spacing: 14, padding: new Insets(16), alignment: Pos.TOP_CENTER) {

                label("FormLayout", style: "-fx-font-size: 20px; -fx-font-weight: bold;")

                formLayout(padding: new Insets(12)) {
                    field(label: "Name") {
                        textField(promptText: "Ada Lovelace")
                    }
                    field(label: "Email", validate: { v ->
                        def s = v?.toString()?.trim()
                        (s && s.contains("@")) ? "" : "Email must contain @"
                    }) {
                        textField(promptText: "ada@example.com")
                    }
                    field(label: "Role", validate: { v -> v ? "" : "Pick a role" }) {
                        comboBox(items: ["User", "Admin", "Owner"])
                    }
                    field(label: "Enabled", validate: { v -> (v == true) ? "" : "Must be enabled" }) {
                        checkBox("Account enabled", selected: true)
                    }
                }

                hbox(spacing: 10, alignment: Pos.CENTER_RIGHT) {
                    button("Cancel")
                    button("Submit")
                }
            }
        }
    }
}
