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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovyx.javafx.canvas.CanvasOperation

/**
 * Factory for CanvasOperation leaf nodes (fill, stroke, lineTo, etc).
 *
 * Fixes JavaFX/Groovy refactor regression where Object.newInstance(...) was used,
 * which incorrectly attempts to instantiate java.lang.Object(builder,name,value,attrs).
 */
class CanvasOperationFactory extends AbstractFXBeanFactory {

    CanvasOperationFactory(Class<? extends CanvasOperation> beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        // IMPORTANT: construct the actual beanClass (CanvasOperation impl)
        def result = super.newInstance(builder, name, value, attributes)

        if (result instanceof CanvasOperation) {
            Map params = null

            // Support:
            //   fill(Color.NAVY)
            //   fill(color: Color.NAVY)
            //   fill([color: Color.NAVY])
            if (value instanceof Map) {
                params = new LinkedHashMap(value as Map)
            } else if (value != null) {
                params = [value: value]
            }

            if (attributes != null && !attributes.isEmpty()) {
                if (params == null) params = new LinkedHashMap()
                params.putAll(attributes)

                // consume attrs so they won't be treated as bean properties
                attributes.clear()
            }

            if (params != null && !params.isEmpty()) {
                result.initParams(params)
            }
        }

        return result
    }
}
