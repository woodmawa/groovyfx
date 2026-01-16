package groovyx.javafx.components

import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView

class RibbonQuickAccessBar extends ToolBar {

    static String iconBasePath = "/demo/icons"  //default for demos

    static void setIconBasePath(String basePath) {
        iconBasePath = basePath
    }

    private static String iconPath(String name) {
        "${iconBasePath}/${name}.png"
    }

    static RibbonQuickAccessBar demoBar() {
        def bar = new RibbonQuickAccessBar()
        bar.items.setAll(
                iconButton("Copy",  iconPath("copy")) { println "QuickAccess: Copy (stub)" },
                iconButton("Cut",   iconPath("cut"))  { println "QuickAccess: Cut (stub)"},
                iconButton("Paste", iconPath("paste")) {println "QuickAccess: Paste (stub)"}
        )
        return bar
    }

    RibbonQuickAccessBar() {
        super()

        styleClass.add("ribbon-quickaccess")
        //for demo call RibbonQuickAccessBar.demoBar()
    }

    private static Button iconButton(String tooltipText, String resourcePath, Closure onAction) {

        // Resolve resource safely (never pass null into Image ctor)
        def is = RibbonQuickAccessBar.class.getResourceAsStream(resourcePath)

        ImageView iv = null
        //defensive checking for null url
        if (is != null) {
            Image img = new Image(is)
            iv = new ImageView(img)

            // Keep icons crisp and consistent
            iv.setFitWidth(14)
            iv.setFitHeight(14)
            iv.setPreserveRatio(true)
            iv.setSmooth(true)
        }

        Button b = new Button()
        if (iv != null) b.graphic = iv
        b.text = null
        b.tooltip = new Tooltip(tooltipText)
        b.focusTraversable = false
        b.styleClass.addAll("ribbon-mini-square", "ribbon-quickaccess-button")
        b.onAction = { onAction.call() }

        return b
    }
}
