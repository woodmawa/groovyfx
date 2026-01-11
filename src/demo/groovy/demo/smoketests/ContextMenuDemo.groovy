package demo.smoketests

import javafx.geometry.Side
import javafx.scene.control.Button

import static groovyx.javafx.GroovyFX.start

start {
    stage(title: "ContextMenu Test", width: 500, height: 300, visible: true) {
        scene {
            borderPane {
                center {
                    button(text: "Context Menu") {
                        def cm = contextMenu {
                            menuItem(text: "Hello", onAction: { println "hello" })
                        }
                        onAction { e ->
                            cm.show((Button) e.source, Side.BOTTOM, 0, 0)
                        }
                    }
                }
            }
        }
    }
}