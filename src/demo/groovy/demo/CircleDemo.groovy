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
import static groovyx.javafx.GroovyFX.start
import javafx.scene.paint.Color

/**
 *
 * @author jimclarke
 */
start {
    stage(title: "Circle (bind) Demo", x: 100, y: 100, visible: true, style: "decorated", primary: true) {

        // attach handler directly to the current Stage (delegate)
        //def st = delegate
        //st.setOnShown {
        onShown {evt ->
            def w = (javafx.stage.Window) evt.source
            def sc = w.scene
            println "window.scene = $sc"
            println "scene.root   = ${sc?.root}"
        }

        // NOTE:
        // Use map attributes (or fill(Color.X)) to set visual properties like fill/stroke.
        // e.g. circle(fill: Color.YELLOW) or circle { fill(Color.YELLOW) }
        //
        // Direct assignment inside the node body (fill = Color.YELLOW) is intentionally
        // not supported, since 'fill' is a reserved DSL name handled by FillFactory.
        scene(width: 400, height: 400, fill: GROOVYBLUE) {
            group {
                circle(radius: 50, centerX: 200, centerY: 200, fill: Color.YELLOW) {

                }
            }
        }
    }
}


