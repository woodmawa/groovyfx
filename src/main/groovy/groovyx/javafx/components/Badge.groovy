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

import javafx.beans.property.StringProperty
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

/**
 * A modern Badge component for status indicators.
 *
 * - Programmatic API: setText(), setBackgroundFill()
 * - Themeable: style classes "groovyfx-badge" + "groovyfx-badge-label"
 * - Safe sizing: background tracks label size with a small extra pad
 */
class Badge extends StackPane {
    private final Label label = new Label()
    private final Rectangle background = new Rectangle()

    private static final double ARC = 10d
    private static final Insets LABEL_PADDING = new Insets(2, 6, 2, 6)
    private static final double BG_EXTRA = 2d

    Badge() {
        styleClass.add("groovyfx-badge")

        // Background pill
        background.arcWidth = ARC
        background.arcHeight = ARC
        background.fill = Color.LIGHTGRAY

        // Text
        label.styleClass.add("groovyfx-badge-label")
        label.padding = LABEL_PADDING
        label.textFill = Color.WHITE
        label.font = Font.font("System", FontWeight.BOLD, 10)

        // Keep background sized to label (including padding), with a little breathing room
        background.widthProperty().bind(label.widthProperty().add(BG_EXTRA))
        background.heightProperty().bind(label.heightProperty().add(BG_EXTRA))

        children.addAll(background, label)
    }

    // --- Backwards-compatible text API ---
    void setText(String text) { label.text = text }
    String getText() { label.text }

    // --- Better binding API ---
    StringProperty textProperty() { label.textProperty() }

    // --- Fill API ---
    void setBackgroundFill(Paint paint) { background.fill = paint }
    Paint getBackgroundFill() { background.fill }
}