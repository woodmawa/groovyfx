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
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.Priority

/**
 * A modern Card component for GroovyFX.
 */
class Card extends VBox {
    private VBox header = new VBox()
    private VBox body = new VBox()
    private VBox footer = new VBox()

    Card() {
        this.getStyleClass().add("groovyfx-card")
        this.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);")
        
        header.getStyleClass().add("card-container")
        body.getStyleClass().add("card-container")
        footer.getStyleClass().add("card-container")

        header.setPadding(new Insets(10))
        body.setPadding(new Insets(10))
        footer.setPadding(new Insets(10))

        VBox.setVgrow(body, Priority.ALWAYS)
        
        this.getChildren().addAll(header, body, footer)
    }

    void setHeader(Node node) {
        header.getChildren().setAll(node)
    }

    void setBody(Node node) {
        body.getChildren().setAll(node)
    }

    void setFooter(Node node) {
        footer.getChildren().setAll(node)
    }
    
    VBox getHeaderContainer() { header }
    VBox getBodyContainer() { body }
    VBox getFooterContainer() { footer }
}
