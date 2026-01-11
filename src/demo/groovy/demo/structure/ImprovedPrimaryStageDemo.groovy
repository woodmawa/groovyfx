package demo.structure

/*
   This example is an improvement over "AllInOne" in that it puts the various aspects
   of UI generation in different places.

   NOTE (GroovyFX modernisation):
   ------------------------------
   This variant uses the explicit `primaryStage { }` DSL node instead of relying on
   implicit primaryStage assignment from `stage { }`.

   Benefits:
     • Clear intent: this is the application's main window
     • No implicit framework behaviour
     • No warnings in modern GroovyFX builds

   The original Improved.groovy is retained for backward-compatibility reference.
*/

import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.beans.FXBindable
import javafx.event.EventHandler

import static groovyx.javafx.GroovyFX.start
import static javafx.geometry.HPos.RIGHT
import static javafx.geometry.VPos.BASELINE

class Email3 {
    @FXBindable String name, address, feedback
    String toString() { "<$name> $address : $feedback" }
}

/**
 * Demonstrates a more structured GroovyFX application using helper methods
 * and shared styling.
 */
start {
    SceneGraphBuilder builder = delegate

    // Explicit call to avoid DSL interception
    this.layoutFrame(builder)

    DemoStyle.style(builder)

    primaryStage.show()
}

def layoutFrame(SceneGraphBuilder sgb) {
    // Preferred modern form: explicitly define the primary stage
    //This demo uses the explicit primaryStage {} DSL.
    //Legacy demos may rely on implicit assignment via stage {}.
    sgb.primaryStage {
        title = 'Improved Demo using primaryStage'

        scene {
            gridPane {
                label id: 'header',
                        row: 0, column: 1,
                        'Please Send Us Your Feedback'

                label 'Name', row: 1, column: 0
                textField id: 'name', row: 1, column: 1

                label 'Address', row: 2, column: 0
                textField id: 'address', row: 2, column: 1

                label 'Feedback',
                        row: 3, column: 0,
                        valignment: BASELINE

                textArea id: 'feedback',
                        row: 3, column: 1

                button id: 'submit',
                        row: 4, column: 1,
                        halignment: RIGHT,
                        'Send Feedback'
            }
        }
    }
}

void bindModelToViews(Email3 email, SceneGraphBuilder sgb) {
    sgb.with {
        email.nameProperty().bind name.textProperty()
        email.addressProperty().bind address.textProperty()
        email.feedbackProperty().bind feedback.textProperty()
    }
}

void attachHandlers(Email3 email, SceneGraphBuilder sgb) {
    sgb.submit.onAction =
            { println "preparing and sending the mail: $email" } as EventHandler
}
