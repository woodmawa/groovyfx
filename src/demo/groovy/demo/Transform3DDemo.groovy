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
import javafx.application.ConditionalFeature
import javafx.application.Platform
import javafx.scene.PerspectiveCamera
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate
import javafx.geometry.Pos


import static groovyx.javafx.GroovyFX.start

start {
    if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
        println "*************************************************************"
        println "*    WARNING: common conditional SCENE3D isn't supported    *"
        println "*************************************************************"
    }

    def cam = new PerspectiveCamera(true)
    cam.nearClip = 0.1
    cam.farClip  = 5000
    cam.translateZ = -1200

    stage(title: "GroovyFX Transform3D Demo", width: 400, height: 300, visible: true, resizable: true) {
        // NOTE: no fill attr here — we paint background via pane style
        scene(camera: cam) {
            group {
                // background (covers whole stage)
                rectangle(x: 0, y: 0, width: 400, height: 300, fill: Color.web("#5f93a3"))

                // the demo rectangle
                rectangle(
                        x: 120, y: 105,
                        width: 160, height: 90,
                        fill: Color.BLUE,
                        transforms: [ new Rotate(30, 120 + 80, 105 + 45, 0, Rotate.Z_AXIS) ]
                )
            }
        }
    }
}