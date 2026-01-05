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

import javafx.animation.TranslateTransition
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.scene.control.SkinBase
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.util.Duration

/**
 * A modern ToggleSwitch component.
 */
class ToggleSwitch extends Control {
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false)

    ToggleSwitch() {
        getStyleClass().add("toggle-switch")
    }

    BooleanProperty selectedProperty() { selected }
    boolean isSelected() { selected.get() }
    void setSelected(boolean selected) { this.selected.set(selected) }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ToggleSwitchSkin(this)
    }

    private static class ToggleSwitchSkin extends SkinBase<ToggleSwitch> {
        private final StackPane container = new StackPane()
        private final Rectangle background = new Rectangle(40, 20)
        private final Circle thumb = new Circle(8)
        private final TranslateTransition transition = new TranslateTransition(Duration.millis(150), thumb)

        ToggleSwitchSkin(ToggleSwitch control) {
            super(control)

            background.setArcWidth(20)
            background.setArcHeight(20)
            background.setFill(control.isSelected() ? Color.web("#4caf50") : Color.web("#bdbdbd"))

            thumb.setFill(Color.WHITE)
            thumb.setTranslateX(control.isSelected() ? 10 : -10)

            container.getChildren().addAll(background, thumb)
            getChildren().add(container)

            container.setOnMouseClicked {
                control.setSelected(!control.isSelected())
            }

            control.selectedProperty().addListener { obs, old, selected ->
                background.setFill(selected ? Color.web("#4caf50") : Color.web("#bdbdbd"))
                transition.setFromX(selected ? -10 : 10)
                transition.setToX(selected ? 10 : -10)
                transition.play()
            }
        }
    }
}
