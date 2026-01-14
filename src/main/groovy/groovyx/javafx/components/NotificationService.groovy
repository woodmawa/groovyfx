package groovyx.javafx.components

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.module.ModuleRegistry
import groovyx.javafx.module.UIModule
import javafx.animation.FadeTransition
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.scene.Node
import javafx.scene.Parent
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
        PopupModuleService.showModule(
                owner,
                DEFAULT_VIEW_MODULE_NAME,
                [owner: owner, message: message, duration: duration],
                duration,
                PopupModuleService.Position.BOTTOM_CENTER,
                100
        )
    }

    static void registerView(UIModule module) {
        ModuleRegistry.register(DEFAULT_VIEW_MODULE_NAME, module)
    }

    /**
     * Install a default notification view module if the app hasn't supplied one.
     * This keeps it self-contained and backward compatible.
     */
    @CompileDynamic
    static void ensureDefaultViewModule() {
        if (ModuleRegistry.has(DEFAULT_VIEW_MODULE_NAME)) return

        ModuleRegistry.register(DEFAULT_VIEW_MODULE_NAME, dslModule { Map ctx ->
            def msg = (ctx.message ?: "").toString()

            stackPane(padding: new javafx.geometry.Insets(20)) {
                label(text: msg) {
                    setStyle("-fx-background-color: #333333; " +
                            "-fx-text-fill: white; " +
                            "-fx-padding: 10px; " +
                            "-fx-background-radius: 5px;")
                }
            }
        })
    }

    // helper methods
    private static UIModule dslModule(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SceneGraphBuilder) Closure<?> dsl
    ) {
        new SceneGraphBuilder().compile(dsl)
    }
}
