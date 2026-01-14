package demo.components

import groovyx.javafx.GroovyFX
import groovyx.javafx.appsupport.FxToolkit
import groovyx.javafx.module.blueprint.Blueprint
import groovyx.javafx.module.blueprint.BlueprintModule
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox

FxToolkit.ensureStarted()

def bp = new Blueprint(
        type: VBox,
        props: [ spacing: 12d, padding: new Insets(16), alignment: Pos.CENTER ],
        children: [
                new Blueprint(type: Label, props: [ text: "Blueprint hooks demo (namespaced)" ]),
                new Blueprint(
                        type: Button,
                        props: [ text: "Click me" ],
                        hooks: [ onAction: "demo.clicked" ]   // ✅ dotted hook name
                )
        ]
)

def module = new BlueprintModule(blueprint: bp)

// ✅ nested handlers map for dotted lookup
def handlers = [
        demo: [
                clicked: { e -> println "Clicked! event=${e.class.name}" }
        ]
]

new GroovyFX().start {
    stage(title: "Blueprint Hook Smoke Test", width: 420, height: 220, visible: true) {
        scene {
            stackPane {
                node(module.build([handlers: handlers]))
            }
        }
    }
}
