package groovyx.javafx.components

import groovy.transform.CompileStatic
import groovyx.javafx.module.CachedModule
import groovyx.javafx.module.ModuleRegistry
import groovyx.javafx.module.UIModule
import javafx.animation.FadeTransition
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.util.Duration

/**
 * Imperative notification/toast runtime (popup, positioning, animation).
 * View creation is module-driven.
 */
@CompileStatic
final class NotificationService {

    static final String DEFAULT_VIEW_MODULE_NAME = "notification.view"

    private NotificationService() {}

    static void show(Stage owner, String message, Duration duration = Duration.seconds(3)) {
        ensureDefaultViewModule()

        UIModule view = ModuleRegistry.get(DEFAULT_VIEW_MODULE_NAME)
        Node root = view.build([
                owner   : owner,
                message : message,
                duration: duration
        ])

        Popup popup = new Popup()
        popup.getContent().add(root)
        popup.show(owner)

        // Positioning (same spirit as your original implementation)
        // Note: width/height may be 0 until CSS/layout pass; this matches the prior simple approach.
        popup.setX(owner.getX() + owner.getWidth() / 2 - root.getBoundsInLocal().getWidth() / 2)
        popup.setY(owner.getY() + owner.getHeight() - 100)

        // Animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root)
        fadeIn.fromValue = 0
        fadeIn.toValue = 1

        PauseTransition pause = new PauseTransition(duration)

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root)
        fadeOut.fromValue = 1
        fadeOut.toValue = 0
        fadeOut.setOnFinished { popup.hide() }

        new SequentialTransition(fadeIn, pause, fadeOut).play()
    }

    /**
     * Install a default notification view module if the app hasn't supplied one.
     * This keeps it self-contained and backward compatible.
     */
    static void ensureDefaultViewModule() {
        if (ModuleRegistry.has(DEFAULT_VIEW_MODULE_NAME)) return

        // Use CachedModule so teams can override by registering a different module at runtime.
        ModuleRegistry.register(DEFAULT_VIEW_MODULE_NAME, new CachedModule({ Map ctx ->
            String msg = (String) ctx.message

            Label label = new Label(msg)
            label.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5px;")

            StackPane root = new StackPane(label)
            root.setPadding(new Insets(20))
            StackPane.setAlignment(label, Pos.CENTER)

            return root
        }))
    }
}
