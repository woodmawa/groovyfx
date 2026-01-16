/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Basic Ribbon demo for GroovyFX (JavaFX 25 rebuild)
 */


import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.layout.GridPane
import groovyx.javafx.components.RibbonBackstageButton
import groovyx.javafx.components.RibbonQuickAccessBar


import static groovyx.javafx.GroovyFX.start

start {
    stage(title: "GroovyFX Ribbon Demo", width: 980, height: 600, visible: true) {

        // THIS IS A NORMAL GROOVY STRING
        final String demoCss =
                getClass().getResource("/demo/demo.css")?.toExternalForm()

        if (!demoCss) {
            throw new RuntimeException("demo.css not found on classpath")
        }

        scene (stylesheets: [demoCss]) {

            /*def css = this.class.getResource("/demo/demo.css")?.toExternalForm()
            if (css == null) throw new RuntimeException("demo.css not found on classpath")
            stylesheets << css*/

            borderPane {

                // --- TOP: Ribbon -------------------------------------------------
                top {
                    // v1: Ribbon is just a node/container with tabs & groups.
                    // Later: you'll likely render tab headers + backstage button.
                    def demoQuickAccess = RibbonQuickAccessBar.demoBar()
                    ribbon(id: 'mainRibbon',
                            collapsible: true,
                            collapsed: false,
                            backstage: null /*new RibbonBackstageButton()*/,  //todo need to know what we want this look like
                            quickAccess: demoQuickAccess) {


                        ribbonTab("Home") {
                            ribbonGroup("Clipboard",
                                    dialogLauncherVisible: true,
                                    onDialogLauncher: { evt ->
                                        def dlg = new javafx.scene.control.Dialog<Void>()
                                        dlg.title = "Clipboard Options"
                                        dlg.headerText = "Clipboard"

                                        def box = new javafx.scene.layout.VBox(10)
                                        box.children.addAll(
                                                new javafx.scene.control.Label("Expanded clipboard settings would go here."),
                                                new javafx.scene.control.CheckBox("Show Office Clipboard"),
                                                new javafx.scene.control.CheckBox("Collect without showing")
                                        )

                                        dlg.dialogPane.content = box
                                        dlg.dialogPane.buttonTypes.add(javafx.scene.control.ButtonType.CLOSE)
                                        dlg.showAndWait()
                                    }
                            ) {
                                button(text: "Paste", onAction: { println "Paste" })
                                button(text: "Cut",   onAction: { println "Cut" })
                                button(text: "Copy",  onAction: { println "Copy" })
                            }

                            ribbonGroup("Font",
                                    dialogLauncherVisible: true,
                                    onDialogLauncher: { evt ->
                                        def dlg = new Dialog<Void>()
                                        dlg.title = "Font"
                                        dlg.headerText = "Font Options"

                                        def grid = new GridPane()
                                        grid.hgap = 10
                                        grid.vgap = 10

                                        grid.add(new Label("Font:"), 0, 0)
                                        grid.add(new ComboBox(["Segoe UI", "Arial", "Consolas"]), 1, 0)

                                        grid.add(new Label("Size:"), 0, 1)
                                        grid.add(new Spinner<Integer>(6, 72, 11), 1, 1)

                                        dlg.dialogPane.content = grid
                                        dlg.dialogPane.buttonTypes.add(ButtonType.CLOSE)
                                        dlg.showAndWait()
                                    }
                            ) {
                                toggleButton(text: "B", onAction: { println "Bold toggled" })
                                toggleButton(text: "I", onAction: { println "Italic toggled" })
                                toggleButton(text: "U", onAction: { println "Underline toggled" })
                            }

                            ribbonGroup("Paragraph") {
                                button(text: "Bullets", onAction: { println "Bullets" })
                                button(text: "Align",   onAction: { println "Align" })
                            }
                        }

                        ribbonTab("Insert") {
                            ribbonGroup("Illustrations") {
                                button(text: "Picture", onAction: { println "Insert Picture" })
                                button(text: "Shapes",  onAction: { println "Insert Shapes" })
                            }
                            ribbonGroup("Media") {
                                button(text: "Video", onAction: { println "Insert Video" })
                                button(text: "Audio", onAction: { println "Insert Audio" })
                            }
                        }

                        ribbonTab("View") {
                            ribbonGroup("Show") {
                                checkBox(text: "Ruler", selected: true, onAction: { println "Ruler toggled" })
                                checkBox(text: "Gridlines", onAction: { println "Gridlines toggled" })
                            }
                        }
                    }
                }

                // --- CENTER: some content so you can see layout ------------------
                center {
                    vbox(spacing: 12, padding: 16) {
                        label(text: "Content Area", style: "-fx-font-size: 18; -fx-font-weight: bold;")
                        label(text: "Use the controls below to test collapsing and selection.")
                        separator()

                        hbox(spacing: 10) {
                            button(text: "Toggle Ribbon Collapse", onAction: {
                                // property access works if you exposed BooleanProperty collapsed
                                mainRibbon.collapsed = !mainRibbon.collapsed
                                println "Ribbon collapsed = ${mainRibbon.collapsed}"
                            })

                            button(text: "Select Home Tab", onAction: {
                                def home = mainRibbon.tabs?.find { it.text == "Home" }
                                if (home) {
                                    mainRibbon.selectedTab = home
                                    println "Selected tab: Home"
                                } else {
                                    println "Tab not found: Home"
                                }
                            })

                            button(text: "Select Insert Tab", onAction: {
                                def insert = mainRibbon.tabs?.find { it.text == "Insert" }
                                if (insert) {
                                    mainRibbon.selectedTab = insert
                                    println "Selected tab: Insert"
                                } else {
                                    println "Tab not found: Insert"
                                }
                            })

                            button(text: "Select View Tab", onAction: {
                                def view = mainRibbon.tabs?.find { it.text == "View" }
                                if (view) {
                                    mainRibbon.selectedTab = view
                                    println "Selected tab: View"
                                } else {
                                    println "Tab not found: View"
                                }
                            })
                        }

                        separator()
                        textArea(
                                promptText: "Type here... (just a filler to show the main UI beneath the ribbon)",
                                prefRowCount: 12
                        )
                    }
                }
            }
        }
    }
}
