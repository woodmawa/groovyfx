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
package groovyx.javafx.factory

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.layout.BorderPane

/**
 * Wrapper passed as a child of BorderPane; ContainerFactory consumes it.
 */
class BorderPanePosition {
    String region
    Node node

    void applyTo(BorderPane pane) {
        if (pane == null || node == null) return
        switch ((region ?: "").toLowerCase()) {
            case "top":
                pane.top = node
                break
            case "bottom":
                pane.bottom = node
                break
            case "left":
                pane.left = node
                break
            case "right":
                pane.right = node
                break
            case "center":
                pane.center = node
                break
            default:
                // Unknown region -> default to center for compatibility
                pane.center = node
                break
        }
    }
}