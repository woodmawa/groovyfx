package groovyx.javafx.components.skin

import groovyx.javafx.components.RibbonGroup
import javafx.animation.PauseTransition
import javafx.collections.ListChangeListener
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.SkinBase
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.Duration

class RibbonGroupSkin extends SkinBase<RibbonGroup> {

    private final VBox root = new VBox(6)
    private final VBox itemsBox = new VBox(6)

    // Footer: label + launcher RHS (no overlap)
    private final HBox footer = new HBox(6)
    private final Label title = new Label()
    private final Region spacer = new Region()
    private final Button launcher = new Button("↘")

    RibbonGroupSkin(RibbonGroup group) {
        super(group)

        root.styleClass.add("ribbon-group-root")
        itemsBox.styleClass.add("ribbon-group-items")
        footer.styleClass.add("ribbon-group-footer")
        title.styleClass.add("ribbon-group-title")
        launcher.styleClass.add("ribbon-group-launcher")

        footer.padding = new Insets(2, 2, 0, 2)
        footer.alignment = Pos.CENTER_LEFT
        HBox.setHgrow(spacer, Priority.ALWAYS)

        // prevent title being squeezed to nothing
        title.minWidth = 0
        title.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(title, Priority.ALWAYS)

        // launcher sizing (hard stop so it can't stretch)
        launcher.focusTraversable = false
        launcher.setMaxSize(14, 14)
        launcher.setMinSize(14, 14)
        launcher.setPrefSize(14, 14)

        // bind title
        title.textProperty().bind(group.textProperty())

        // launcher visibility
        launcher.visibleProperty().bind(group.dialogLauncherVisibleProperty())
        launcher.managedProperty().bind(group.dialogLauncherVisibleProperty())

        launcher.setOnAction { ActionEvent evt ->
            flashLauncher()
            def h = group.getOnDialogLauncher()
            if (h != null) h.handle(evt)
        }

        footer.children.setAll(title, spacer, launcher)
        root.children.setAll(itemsBox, footer)
        getChildren().add(root)

        rebuildItems(group)

        group.items.addListener(({ ListChangeListener.Change<? extends javafx.scene.Node> c ->
            rebuildItems(group)
        } as ListChangeListener<javafx.scene.Node>))
    }

    private void rebuildItems(RibbonGroup group) {
        itemsBox.children.setAll(group.items)
    }

    private void flashLauncher() {
        if (!launcher.styleClass.contains("flash")) launcher.styleClass.add("flash")

        // make flash easier to see while tuning
        def pt = new PauseTransition(Duration.millis(180))
        pt.setOnFinished { launcher.styleClass.remove("flash") }
        pt.play()
    }
}
