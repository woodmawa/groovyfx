package groovyx.javafx.components.palette

/**
 * Non-UI model class.
 * Not a JavaFX Node and does not require a SceneGraphBuilder factory.
 */

class PaletteGroup {
    String label
    List<PaletteItem> items = []
}