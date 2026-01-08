/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright ...
 */
package groovyx.javafx.canvas

import groovyx.javafx.beans.FXBindable
import javafx.scene.canvas.GraphicsContext

@FXBindable
class SetLineWidthOperation implements CanvasOperation, OpParamCoercion {

    double lw

    // TODO(groovyfx-refactor): use OpParamCoercion pick()/unwrap()/coerce() to accept Map + BindingHolder params

    void initParams(Object params) {
        def raw = pick(params, ['lw','lineWidth','width','value'])
        def v = coerce(raw, Double)
        if (v == null) throw new IllegalArgumentException("lineWidth requires a value (got null)")
        lw = v
    }
    void execute(GraphicsContext gc) {
        gc.setLineWidth(lw)
    }


}

