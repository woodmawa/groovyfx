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
import javafx.scene.canvas.GraphicsContext
import javafx.scene.transform.Affine
import javafx.scene.transform.Transform

/**
 *
 * @author jimclarke
 */
@FXBindable
class TransformOperation implements CanvasOperation, OpParamCoercion {
    double mxx
    double myx
    double mxy
    double myy
    double mxt
    double myt
    Affine xform
    

    void initParams(Object params) {
        def raw = pick(params, ['transform','affine','value'])

        raw = unwrap(raw)

        if (raw instanceof Transform) {
            transform = (raw instanceof Affine) ? (Affine) raw : new Affine(raw)
            return
        }

        if (params instanceof Map) {
            def mxx = coerce(pick(params, ['mxx']), Double)
            def mxy = coerce(pick(params, ['mxy']), Double)
            def myx = coerce(pick(params, ['myx']), Double)
            def myy = coerce(pick(params, ['myy']), Double)
            def tx  = coerce(pick(params, ['tx','x']), Double)
            def ty  = coerce(pick(params, ['ty','y']), Double)
            transform = new Affine(mxx, mxy, tx, myx, myy, ty)
            return
        }

        def list = asListish(raw)
        if (list && list.size() >= 6) {
            transform = new Affine(
                    coerce(list[0], Double), coerce(list[2], Double), coerce(list[4], Double),
                    coerce(list[1], Double), coerce(list[3], Double), coerce(list[5], Double)
            )
            return
        }

        throw new IllegalArgumentException("transform expects Transform/Affine or 6 numbers or map {mxx,mxy,myx,myy,tx,ty}, got: $params")
    }

    public void execute(GraphicsContext gc) {
        if(xform != null)
            gc.transform(xform)
        else
            gc.transform(mxx, myx, mxy, myy, mxt, myt);
    }
}


