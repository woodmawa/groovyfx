package groovyx.javafx.components

import groovy.transform.CompileStatic
import groovyx.javafx.module.ModuleRegistry
import groovyx.javafx.module.UIModule
import javafx.animation.FadeTransition
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.Parent
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.util.Duration

@CompileStatic
final class PopupModuleService {

    private PopupModuleService() {}

    enum Position {
        BOTTOM_CENTER,
        TOP_RIGHT,
        CENTER
    }

    static void showModule(
            Stage owner,
            String moduleName,
            Map ctx = [:],
            Duration duration = Duration.seconds(3),
            Position position = Position.BOTTOM_CENTER,
            double yOffset = 100,
            Duration fadeIn = Duration.millis(300),
            Duration fadeOut = Duration.millis(300)
    ) {
        UIModule view = ModuleRegistry.get(moduleName)
        Node root = view.build(ctx ?: [:])

        // Ensure CSS + layout have run so bounds are valid before positioning.
        root.applyCss()
        if (root instanceof Parent) {
            ((Parent) root).layout()
        }

        Popup popup = new Popup()
        popup.content.add(root)
        popup.show(owner)

        positionPopup(owner, popup, root, position, yOffset)

        FadeTransition ftIn = new FadeTransition(fadeIn, root)
        ftIn.fromValue = 0
        ftIn.toValue = 1

        PauseTransition pause = new PauseTransition(duration)

        FadeTransition ftOut = new FadeTransition(fadeOut, root)
        ftOut.fromValue = 1
        ftOut.toValue = 0
        ftOut.onFinished = { popup.hide() }

        new SequentialTransition(ftIn, pause, ftOut).play()
    }

    private static void positionPopup(Stage owner, Popup popup, Node root, Position position, double yOffset) {
        Bounds b = root.boundsInLocal
        double w = b.width
        double h = b.height

        switch (position) {
            case Position.BOTTOM_CENTER:
                popup.x = owner.x + owner.width / 2d - w / 2d
                popup.y = owner.y + owner.height - yOffset
                break

            case Position.TOP_RIGHT:
                popup.x = owner.x + owner.width - w - 20
                popup.y = owner.y + 20
                break

            case Position.CENTER:
                popup.x = owner.x + owner.width / 2d - w / 2d
                popup.y = owner.y + owner.height / 2d - h / 2d
                break
        }
    }
}
