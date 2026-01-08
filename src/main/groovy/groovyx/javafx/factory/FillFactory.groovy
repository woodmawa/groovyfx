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

import javafx.scene.paint.Paint

/**
 * A factory to create fill nodes. A fill node is a leaf node that can be placed under Shapes
 * or any node with a fill property.
 */
class FillFactory extends AbstractFXBeanFactory {

    FillFactory() {
        super(Paint, true)
    }

    FillFactory(Class<Paint> beanClass) {
        super(beanClass, true)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // Accept value from either argument or attribute (common builder pattern)
        def v = (value != null) ? value : (attributes ? attributes.remove('value') : null)

        // IMPORTANT: unwrap buildable specs (e.g. RadialGradientFactory$Spec, LinearGradientFactory$Spec, etc.)
        if (v != null && v.metaClass?.respondsTo(v, 'build')) {
            v = v.build()
        }

        Paint paint = ColorFactory.get(v)

        if (!paint) {
            throw new RuntimeException(
                    "The value passed to the 'fill' node must be an instance of Paint (or a buildable paint spec)."
            )
        }
        return paint
    }

    @Override
    void setParent(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child) {
            FXHelper.setPropertyOrMethod(parent, "fill", child)
        }
    }
}
