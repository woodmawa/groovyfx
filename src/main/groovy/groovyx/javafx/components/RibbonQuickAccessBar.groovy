package groovyx.javafx.components

import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView

class RibbonQuickAccessBar extends ToolBar {

    RibbonQuickAccessBar() {
        super()

        styleClass.add("ribbon-quickaccess")

        def copyButton  = iconButton("Copy",  "/demo/icons/copy.png")  { println "QuickAccess: Copy (stub)" }
        def cutButton   = iconButton("Cut",   "/demo/icons/cut.png")   { println "QuickAccess: Cut (stub)" }
        def pasteButton = iconButton("Paste", "/demo/icons/paste.png") { println "QuickAccess: Paste (stub)" }

        items.setAll(copyButton, cutButton, pasteButton)
        // If you want a separator before tabs, do it in RibbonSkin (leftBox), not here.
        // items.add(new Separator(Orientation.VERTICAL))
    }

    private static Button iconButton(String tooltipText, String resourcePath, Closure onAction) {
        Image img = new Image(RibbonQuickAccessBar.class.getResourceAsStream(resourcePath))
        ImageView iv = new ImageView(img)

        // Keep icons crisp and consistent
        iv.setFitWidth(14)
        iv.setFitHeight(14)
        iv.setPreserveRatio(true)
        iv.setSmooth(true)

        Button b = new Button()
        b.graphic = iv
        b.text = null
        b.tooltip = new Tooltip(tooltipText)
        b.focusTraversable = false
        b.styleClass.addAll("ribbon-mini-square", "ribbon-quickaccess-button")
        b.onAction = { onAction.call() }

        return b
    }
}
