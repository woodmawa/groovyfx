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

import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.RowConstraints
import java.util.Locale

/**
 * Creates ColumnConstraints / RowConstraints nodes.
 *
 * Fixes a Groovy 4+ / stricter reflection path where beanClass may be Object,
 * which causes FactoryBuilderSupport to try to call Object(builder,name,value,map).
 */
class GridConstraintFactory extends AbstractFXBeanFactory {

    private final Class<?> constraintClass

    GridConstraintFactory(Class<?> constraintClass = null) {
        // IMPORTANT: pass something non-null to super when possible
        super(constraintClass ?: Object)
        this.constraintClass = constraintClass
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        Class<?> cls = (constraintClass && constraintClass != Object) ? constraintClass : resolveFromName(name)

        if (cls == null || cls == Object) {
            throw new IllegalArgumentException("GridConstraintFactory cannot resolve constraint type for node: ${name}")
        }

        // ColumnConstraints/RowConstraints are normal JavaFX beans with no-arg ctor
        def obj = cls.getDeclaredConstructor().newInstance()

        // Apply attributes directly, but coerce enum-typed properties from strings (e.g. "right" -> HPos.RIGHT)
        if (attributes) {
            attributes.each { k, v ->
                def mp = obj.metaClass.getMetaProperty(k as String)
                if (mp && v instanceof CharSequence) {
                    Class t = mp.type
                    if (t != null && t.isEnum()) {
                        String s = v.toString().trim()
                        // Common GroovyFX style: allow lower-case enum names
                        v = java.lang.Enum.valueOf(t, s.toUpperCase(Locale.ROOT))
                    }
                }
                obj."$k" = v
            }
            attributes.clear()
        }

        return obj
    }

    private static Class<?> resolveFromName(Object name) {
        switch (name?.toString()) {
            case 'columnConstraints': return ColumnConstraints
            case 'rowConstraints':    return RowConstraints
            default:                  return Object
        }
    }
}
