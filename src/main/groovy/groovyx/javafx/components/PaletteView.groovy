package groovyx.javafx.components

import groovyx.javafx.components.action.Action
import groovyx.javafx.components.palette.PaletteGroup
import groovyx.javafx.components.palette.PaletteItem
import groovyx.javafx.components.palette.PaletteModel
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox

/**
 * Generic palette container:
 * - Left rail (Create/Explore)
 * - Top mini toolbar (expand/collapse)
 * - Center grouped tool list (TreeView)
 *
 * App provides a PaletteModel and listens to events.
 */
class PaletteView extends BorderPane {

    enum Mode { CREATE, EXPLORE }

    final ObjectProperty<Mode> modeProperty = new SimpleObjectProperty<>(Mode.CREATE)
    final ObjectProperty<PaletteModel> modelProperty = new SimpleObjectProperty<>(null)

    /** Fires on single selection of a leaf item */
    Closure onItemSelected

    /** Fires on activation of a leaf item (double-click or Enter) */
    Closure onItemActivated

    // UI parts
    private final ToggleButton createBtn = new ToggleButton("Create")
    private final ToggleButton exploreBtn = new ToggleButton("Explore")
    private final TreeView<Object> tree = new TreeView<>()
    private final TreeItem<Object> root = new TreeItem<>("ROOT")

    PaletteView() {
        buildUI()
        wire()
    }

    void setModel(PaletteModel model) { modelProperty.set(model) }
    PaletteModel getModel() { modelProperty.get() }

    Mode getMode() { modeProperty.get() }
    void setMode(Mode m) { modeProperty.set(m) }

    private void buildUI() {

        // ---- top mini toolbar
        def expandAllBtn = new Button("+")
        expandAllBtn.tooltip = new Tooltip("Expand all")

        def collapseAllBtn = new Button("-")
        collapseAllBtn.tooltip = new Tooltip("Collapse all")

        def tb = new ToolBar(expandAllBtn, collapseAllBtn)
        setTop(tb)

        // ---- left rail
        def tg = new ToggleGroup()
        createBtn.toggleGroup = tg
        exploreBtn.toggleGroup = tg
        createBtn.selected = true

        createBtn.maxWidth = Double.MAX_VALUE
        exploreBtn.maxWidth = Double.MAX_VALUE

        def rail = new VBox(createBtn, exploreBtn)
        rail.styleClass.add("palette-rail")
        setLeft(rail)

        // ---- center tree
        tree.root = root
        tree.showRoot = false
        setCenter(tree)

        // Expand/collapse behavior
        expandAllBtn.setOnAction { expandAll(root, true) }
        collapseAllBtn.setOnAction { expandAll(root, false) }
    }

    private void wire() {

        // mode updates
        createBtn.setOnAction { modeProperty.set(Mode.CREATE) }
        exploreBtn.setOnAction { modeProperty.set(Mode.EXPLORE) }

        // rebuild tree when model changes (or mode changes if you provide different models per mode)
        modelProperty.addListener ( { ObservableValue obs, oldV, newV ->
            rebuildTree(newV)
        }  as ChangeListener<PaletteModel> )

        // selection event
        tree.selectionModel.selectedItemProperty().addListener ({ ObservableValue obs, oldV, newV ->
            def v = newV?.value
            if (v instanceof PaletteItem) {
                onItemSelected?.call(v)
            }
        }  as ChangeListener<TreeItem> )

        // activation (double click)
        tree.setOnMouseClicked { evt ->
            if (evt.clickCount < 2) return
            def item = tree.selectionModel.selectedItem
            if (item?.value instanceof PaletteItem) {
                onItemActivated?.call(item.value)
            }
            evt.consume()
        }

        // activation (Enter key)
        tree.setOnKeyPressed { evt ->
            if (evt.code?.name() != "ENTER") return
            def item = tree.selectionModel.selectedItem
            if (item?.value instanceof PaletteItem) {
                onItemActivated?.call(item.value)
            }
            evt.consume()
        }
    }

    private void rebuildTree(PaletteModel model) {
        root.children.clear()
        if (model == null) return

        model.groups?.each { PaletteGroup g ->
            def gItem = new TreeItem<Object>(g)
            gItem.expanded = true

            g.items?.each { PaletteItem it ->
                def leaf = new TreeItem<Object>(it)
                gItem.children.add(leaf)
            }
            root.children.add(gItem)
        }

        // cell rendering: show label for groups/items
        tree.setCellFactory { tv ->
            new TreeCell<Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        graphic = null
                        disable = false
                        return
                    }
                    if (item instanceof PaletteGroup) {
                        text = ((PaletteGroup)item).label
                        disable = false
                    } else if (item instanceof PaletteItem) {
                        def pi = (PaletteItem)item
                        text = pi.label
                        disable = !pi.enabled
                    } else {
                        text = item.toString()
                        disable = false
                    }
                }
            }
        }
    }

    private static void expandAll(TreeItem<?> item, boolean expanded) {
        if (item == null) return
        item.expanded = expanded
        item.children?.each { expandAll(it, expanded) }
    }
}
