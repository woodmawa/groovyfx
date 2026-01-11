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
import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate

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
    cam.translateZ = -800

    stage(title: "GroovyFX Transform3D Demo", width: 400, height: 300, visible: true) {
        scene(fill: Color.web("#5f93a3"), depthBuffer: true, camera: cam) {

            // IMPORTANT: Group for 3D (no layout meddling)
            group {
                def w = 160
                def h = 90

                rectangle(
                        // center the rectangle on the 3D origin:
                        x: -w/2, y: -h/2,
                        width: w, height: h,
                        fill: Color.BLUE,

                        // bring it forward a bit so perspective is obvious:
                        translateZ: 200,

                        // rotate around the origin (which is now its center):
                        rotationAxis: Rotate.Y_AXIS,
                        rotate: 35
                )
            }
        }
    }

}

/*

Platform.runLater {
            // 1) center the camera similarly to the default camera behavior
            // cam.translateX = sc.width / 2.0
            //cam.translateY = sc.height / 2.0

            // 2) put the rectangle centered in the scene (in 2D), then add 3D
            r.layoutX = (sc.width  - r.width)  / 2.0
            r.layoutY = (sc.height - r.height) / 2.0

            // 3) apply transforms *after* placing it
            r.transforms.setAll(
                    new Rotate(45, r.width/2.0, r.height/2.0, 0, Rotate.Y_AXIS),
                    new Translate(0, 0, 300)
            )

            println "Scene size: ${sc.width} x ${sc.height}"
            println "Camera: tx=${cam.translateX}, ty=${cam.translateY}, tz=${cam.translateZ}"
            println "Rect layout: layoutX=${r.layoutX}, layoutY=${r.layoutY}"
            def b = r.localToScene(r.boundsInLocal)
            println "Rect bounds in scene: minX=${b.minX}, minY=${b.minY}, w=${b.width}, h=${b.height}"
        }
 */