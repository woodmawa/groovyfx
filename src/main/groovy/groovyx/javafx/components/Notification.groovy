/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.javafx.components

import javafx.animation.FadeTransition
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.util.Duration

/**
 * A non-blocking Notification (Toast) system.
 */
class Notification {
    static void show(Stage owner, String message, Duration duration = Duration.seconds(3)) {
        Popup popup = new Popup()
        
        Label label = new Label(message)
        label.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5px;")
        
        StackPane root = new StackPane(label)
        root.setPadding(new Insets(20))
        popup.getContent().add(root)
        
        popup.show(owner)
        
        // Positioning
        popup.setX(owner.getX() + owner.getWidth() / 2 - root.getWidth() / 2)
        popup.setY(owner.getY() + owner.getHeight() - 100)

        // Animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root)
        fadeIn.setFromValue(0)
        fadeIn.setToValue(1)
        
        PauseTransition pause = new PauseTransition(duration)
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root)
        fadeOut.setFromValue(1)
        fadeOut.setToValue(0)
        fadeOut.setOnFinished { popup.hide() }
        
        new SequentialTransition(fadeIn, pause, fadeOut).play()
    }
}
