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
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color

/**
 * A responsive FormLayout that aligns labels and inputs.
 */
class FormLayout extends GridPane {
    private int currentRow = 0
    private boolean responsive = false

    FormLayout() {
        setHgap(10)
        setVgap(10)
        setPadding(new Insets(10))

        widthProperty().addListener { obs, old, val ->
            if (responsive) {
                updateResponsiveLayout(val.doubleValue())
            }
        }
    }

    void setResponsive(boolean responsive) {
        this.responsive = responsive
    }

    private void updateResponsiveLayout(double width) {
        if (width < 400) { // Small breakpoint
            // In a real implementation, we would switch from 2 columns to 1 column
            // For now, we'll just adjust hgap/vgap or inform the user
        }
    }

    void addField(String labelText, Node field) {
        Label label = new Label(labelText)
        GridPane.setHalignment(label, javafx.geometry.HPos.RIGHT)
        add(label, 0, currentRow)
        
        GridPane.setHgrow(field, Priority.ALWAYS)
        add(field, 1, currentRow)
        
        currentRow++
    }

    void addField(String labelText, Node field, Label errorLabel) {
        Label label = new Label(labelText)
        GridPane.setHalignment(label, javafx.geometry.HPos.RIGHT)
        add(label, 0, currentRow)

        GridPane.setHgrow(field, Priority.ALWAYS)
        add(field, 1, currentRow)
        
        errorLabel.setTextFill(Color.RED)
        errorLabel.setStyle("-fx-font-size: 0.8em;")
        add(errorLabel, 1, currentRow + 1)
        
        currentRow += 2
    }
}
