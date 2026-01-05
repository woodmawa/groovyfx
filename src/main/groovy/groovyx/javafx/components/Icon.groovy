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

import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.SVGPath
import javafx.beans.property.StringProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

/**
 * A modern Icon component that supports SVG paths.
 */
class Icon extends StackPane {
    private final SVGPath path = new SVGPath()
    private final StringProperty iconName = new SimpleStringProperty(this, "iconName")
    private final ObjectProperty<Paint> fill = new SimpleObjectProperty<>(this, "fill", Color.BLACK)

    // A small built-in library of common icons
    private static final Map<String, String> LIBRARY = [
        "save": "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z",
        "edit": "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
        "delete": "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
        "add": "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
        "info": "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
        "warning": "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z",
        "check": "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
    ]

    Icon() {
        getChildren().add(path)
        path.fillProperty().bind(fill)
        
        iconName.addListener { obs, old, name ->
            if (LIBRARY.containsKey(name)) {
                path.setContent(LIBRARY[name])
            }
        }
    }

    StringProperty iconNameProperty() { iconName }
    String getIconName() { iconName.get() }
    void setIconName(String name) { iconName.set(name) }

    ObjectProperty<Paint> fillProperty() { fill }
    Paint getFill() { fill.get() }
    void setFill(Paint fill) { this.fill.set(fill) }

    void setContent(String content) {
        path.setContent(content)
    }
}
