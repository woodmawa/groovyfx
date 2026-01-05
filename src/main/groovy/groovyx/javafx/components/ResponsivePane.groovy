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
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Pane
import javafx.scene.Node

/**
 * A Pane that rearranges its children based on available width.
 */
class ResponsivePane extends FlowPane {
    private final DoubleProperty breakpoint = new SimpleDoubleProperty(this, "breakpoint", 600.0)

    ResponsivePane() {
        setHgap(10)
        setVgap(10)
        
        widthProperty().addListener { obs, old, val ->
            updateLayout(val.doubleValue())
        }
    }

    DoubleProperty breakpointProperty() { breakpoint }
    double getBreakpoint() { breakpoint.get() }
    void setBreakpoint(double breakpoint) { this.breakpoint.set(breakpoint) }

    private void updateLayout(double width) {
        if (width < getBreakpoint()) {
            setPrefWrapLength(0) // Force vertical
        } else {
            setPrefWrapLength(width) // Allow horizontal
        }
    }
}
