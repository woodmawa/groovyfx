package demo.components

import groovyx.javafx.module.blueprint.Blueprint
import groovyx.javafx.module.blueprint.BlueprintModule
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import groovyx.javafx.appsupport.FxToolkit


FxToolkit.ensureStarted()


def bp = new Blueprint(
        type: StackPane,
        children: [
                new Blueprint(
                        type: Label,
                        props: [ text: { ctx -> ctx.message } ]
                )
        ]
)

def m = new BlueprintModule(blueprint: bp)

println m.blueprint
println m.blueprint.type
println m.build([message: "Hello"]).class