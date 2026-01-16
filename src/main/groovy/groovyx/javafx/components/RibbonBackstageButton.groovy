package groovyx.javafx.components

import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem

/**
 * Visible, stable stub for the Ribbon "Backstage" button.
 * Opens the File/Backstage area (future).
 */
class RibbonBackstageButton extends MenuButton {

    RibbonBackstageButton() {
        super("File")


        getStyleClass().addAll("ribbon-backstage", "ribbon-header-holder", "ribbon-mini-square")

        // Placeholder items (no real actions yet)
        def open = new MenuItem("Open")
        def saveAs = new MenuItem("Save As")
        def print = new MenuItem("Print")
        def exit = new MenuItem("Exit")

        items.setAll(
                open,
                saveAs,
                new SeparatorMenuItem(),
                print,
                new SeparatorMenuItem(),
                exit
        )

        // Keep it non-invasive for demos
        setFocusTraversable(false)
    }
}
