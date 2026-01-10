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

import groovyx.javafx.canvas.CanvasOperation

/**
 * Supports:
 *   canvas { operation(value: someClosure) }
 *
 * The CanvasFactory only records children that are instances of
 * groovyx.javafx.canvas.CanvasOperation, so this factory must return one.
 *
 * NOTE: We intentionally rely on Groovy SAM coercion:
 *   Closure -> CanvasOperation
 * so we don't need to know the exact method name on CanvasOperation.
 */
class CanvasClosureOperationFactory extends AbstractFXBeanFactory {

    CanvasClosureOperationFactory() {
        super(Object)
    }

    CanvasClosureOperationFactory(Class beanClass) {
        // SceneGraphBuilder.registerCanvas() constructs us with (Class)
        super(beanClass ?: Object)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        def op = value
        if (op == null && attributes != null) {
            // demo-friendly: operation(value: drawCanvas)
            op = attributes.remove('value')
        }

        if (!(op instanceof Closure)) {
            throw new IllegalArgumentException("operation requires a Closure (e.g. operation(value: drawCanvas))")
        }

        // Make sure the closure looks like the expected operation signature
        // (GraphicsContext parameter). Even if it's not typed, SAM coercion works.
        return ((Closure) op) as CanvasOperation
    }
}
