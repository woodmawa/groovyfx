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
package groovyx.javafx.canvas

import groovyx.javafx.beans.FXBindable
import javafx.scene.canvas.GraphicsContext;

/**
 *
 * @author jimclarke
 */
@FXBindable
class ScaleOperation implements CanvasOperation, OpParamCoercion {
    double x
    double y

    void initParams(Object params) {
        if (params instanceof Map) {
            def mx = pick(params, ['x','sx','scaleX'])
            def my = pick(params, ['y','sy','scaleY'])
            def xVal = coerce(mx, Double)
            def yVal = coerce(my, Double)
            if (xVal != null && yVal == null) yVal = xVal
            scaleX = xVal
            scaleY = yVal
            return
        }

        def list = asListish(unwrap(params))
        if (list && list.size() >= 2) {
            scaleX = coerce(list[0], Double)
            scaleY = coerce(list[1], Double)
            return
        }

        def v = coerce(params, Double)
        scaleX = v
        scaleY = v
    }

    public void execute(GraphicsContext gc) {
        gc.scale(x, y);
    }
}


