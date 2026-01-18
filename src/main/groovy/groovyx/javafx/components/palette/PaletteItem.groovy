package groovyx.javafx.components.palette

/**
 * Non-UI model class.
 * Not a JavaFX Node and does not require a SceneGraphBuilder factory.
 */

class PaletteItem {
    String id          // e.g. "class", "interface"
    String label       // UI text
    String icon        // optional iconName
    boolean enabled = true
}