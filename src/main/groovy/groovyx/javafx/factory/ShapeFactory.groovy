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

import groovyx.javafx.binding.BindingHolder
import javafx.beans.value.ObservableValue
import javafx.scene.shape.Shape
import groovy.util.FactoryBuilderSupport

class ShapeFactory extends AbstractNodeFactory {

    ShapeFactory(shapeClass) {
        super(shapeClass, false)
    }

    @Override
    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        def shape = (Shape) node

        // --- fill ---
        if (attributes.containsKey('fill')) {
            def v = attributes.remove('fill')
            // If it's a binding holder / observable, let the base class bind it.
            if (v instanceof BindingHolder || v instanceof ObservableValue) {
                attributes.put('fill', v)
            } else {
                shape.fill = ColorFactory.get(v)
            }
        }

        // --- stroke ---
        if (attributes.containsKey('stroke')) {
            def v = attributes.remove('stroke')
            if (v instanceof BindingHolder || v instanceof ObservableValue) {
                attributes.put('stroke', v)
            } else {
                shape.stroke = ColorFactory.get(v)
            }
        }

        // Continue normal processing for everything else
        return super.onHandleNodeAttributes(builder, node, attributes)
    }
}