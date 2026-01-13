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

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.geometry.Bounds
import javafx.geometry.Pos
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.FillRule
import javafx.scene.shape.SVGPath

/**
 * A modern Icon component that supports SVG paths.
 *
 * DSL:
 *   icon(name: "info", size: 24, fill: Color.DODGERBLUE)
 *   icon(path: "check")   // alias for name
 *   icon(iconName: "add")
 */
class Icon extends StackPane {

    private final SVGPath svg = new SVGPath()

    private final StringProperty iconName = new SimpleStringProperty(this, "iconName")
    private final ObjectProperty<Paint> fill = new SimpleObjectProperty<>(this, "fill", Color.BLACK)
    private final DoubleProperty size = new SimpleDoubleProperty(this, "size", 16d)

    // NOTE: These must be full, valid JavaFX SVGPath 'content' strings.
    private static final Map<String, String> LIBRARY = [
            // Material-ish (valid as JavaFX SVGPath content)
            "check"  : "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
            "add"    : "M19 13H13V19H11V13H5V11H11V5H13V11H19z",
            "info"   : "M11 7H13V9H11z M11 11H13V17H11z M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z",
            "warning": "M1 21H23L12 2 1 21z M13 18H11V16H13z M13 14H11V10H13z"
    ]

    Icon() {
        alignment = Pos.CENTER
        children.add(svg)

        svg.fillProperty().bind(fill)
        svg.fillRule = FillRule.NON_ZERO

        // Keep the control itself sized; makes layout predictable in VBox/HBox/etc.
        // Also makes it easier to click/align if you later add tooltips etc.
        size.addListener { _, _, n ->
            double s = (n as Number).doubleValue()
            setMinSize(s, s)
            setPrefSize(s, s)
            setMaxSize(s, s)
            rescaleToFit()
        }
        // initialize size box
        double s0 = size.get()
        setMinSize(s0, s0)
        setPrefSize(s0, s0)
        setMaxSize(s0, s0)

        iconName.addListener { _, _, n ->
            String key = n as String
            if (key && LIBRARY.containsKey(key)) {
                svg.content = LIBRARY[key]
            } else {
                // Allow passing raw SVG path data directly via iconName/path/name
                svg.content = key ?: ""
            }
            rescaleToFit()
        }
    }

    // --- Primary API ---
    StringProperty iconNameProperty() { iconName }
    String getIconName() { iconName.get() }
    void setIconName(String name) { iconName.set(name) }

    ObjectProperty<Paint> fillProperty() { fill }
    Paint getFill() { fill.get() }
    void setFill(Paint p) { fill.set(p) }

    DoubleProperty sizeProperty() { size }
    double getSize() { size.get() }
    void setSize(double v) { size.set(v) }

    /** Set raw SVG path content directly. */
    void setContent(String content) {
        svg.content = content ?: ""
        rescaleToFit()
    }

    // --- DSL aliases ---
    String getPath() { getIconName() }
    void setPath(String p) { setIconName(p) }

    String getName() { getIconName() }
    void setName(String n) { setIconName(n) }

    // --- internals ---
    private void rescaleToFit() {
        // If content is empty/invalid, bounds can be 0; avoid divide-by-zero.
        Bounds b = svg.layoutBounds
        double w = b?.width ?: 0d
        double h = b?.height ?: 0d
        if (w <= 0d || h <= 0d) {
            svg.scaleX = 1d
            svg.scaleY = 1d
            return
        }

        double s = size.get()
        // Fit inside square, leaving a tiny padding so strokes/edges don’t clip.
        double pad = Math.max(1d, s * 0.06d)
        double target = Math.max(1d, s - pad)

        double scale = Math.min(target / w, target / h)
        svg.scaleX = scale
        svg.scaleY = scale
    }
}
