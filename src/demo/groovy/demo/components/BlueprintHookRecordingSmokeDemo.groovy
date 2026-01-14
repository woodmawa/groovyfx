package demo.components

import groovyx.javafx.GroovyFX
import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.appsupport.FxToolkit
import javafx.scene.Node
import javafx.scene.control.Button

FxToolkit.ensureStarted()

// Step B: record Blueprint from DSL, return BlueprintModule
def module = SceneGraphBuilder.blueprint {
    vbox(spacing: 12d, padding: 16, alignment: "CENTER") {
        label(text: "Blueprint hooks demo (recorded from DSL)")
        button(text: "Click me", onAction: "demo.clicked")
    }
}

// ✅ nested handlers map for dotted lookup
// ✅ standardized signature: (event, ctx, node)
def handlers = [
        demo: [
                clicked: { e, ctx, Node node ->
                    def nodeType = node?.class?.simpleName
                    def eventType = e?.class?.simpleName

                    def hasHandlers = (ctx?.handlers != null)
                    def tag = ctx?.tag

                    def buttonText = (node instanceof Button) ? ((Button) node).text : "(not a Button)"

                    println "Clicked! event=${eventType}, node=${nodeType}, buttonText='${buttonText}', ctx.tag=${tag}, ctx.hasHandlers=${hasHandlers}"
                }
        ]
]

new GroovyFX().start {
    stage(title: "Blueprint Hook Smoke Test (Recorded DSL)", width: 420, height: 220, visible: true) {
        scene {
            stackPane {
                node(module.build([
                        handlers: handlers,
                        tag: "StepB-hook-context-demo"
                ]))
            }
        }
    }
}
