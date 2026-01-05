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

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

/**
 * A modern Badge component for status indicators.
 */
class Badge extends StackPane {
    private Label label = new Label()
    private Rectangle background = new Rectangle()

    Badge() {
        this.getStyleClass().add("groovyfx-badge")
        
        background.setArcWidth(10)
        background.setArcHeight(10)
        background.setFill(Color.LIGHTGRAY)
        
        label.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;")
        label.setPadding(new Insets(2, 5, 2, 5))
        
        background.widthProperty().bind(label.widthProperty())
        background.heightProperty().bind(label.heightProperty())
        
        this.getChildren().addAll(background, label)
    }

    void setText(String text) {
        label.setText(text)
    }

    String getText() {
        label.getText()
    }

    void setBackgroundFill(Color color) {
        background.setFill(color)
    }
}
