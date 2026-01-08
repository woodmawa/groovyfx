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

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.Pane

/**
 * SceneFactory - creates a JavaFX Scene and sets its root.
 *
 * Supports:
 * - scene(width:..., height:...) { ... }
 * - nested nodes added to a default Group root
 * - stylesheet children as Strings or StylesheetRef
 */
class SceneFactory extends AbstractNodeFactory {

    SceneFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {

        // --- GroovyFX legacy: allow id: 'foo' on Scene to mean "register var foo"
        def varId = attributes.remove('id')

        // normal Scene creation
        def root = null
        if (value instanceof javafx.scene.Parent) {
            root = value
        } else if (attributes.containsKey('root')) {
            root = attributes.remove('root')
        }

        def width  = attributes.containsKey('width')  ? (attributes.remove('width')  as Number).doubleValue()  : 0d
        def height = attributes.containsKey('height') ? (attributes.remove('height') as Number).doubleValue() : 0d

        def fill = attributes.remove('fill')  // may be Color/Paint/etc (your FillFactory should already produce Paint)

        javafx.scene.Scene scene
        if (root != null && width > 0 && height > 0 && fill != null) {
            scene = new javafx.scene.Scene(root, width, height, (javafx.scene.paint.Paint) fill)
        } else if (root != null && width > 0 && height > 0) {
            scene = new javafx.scene.Scene(root, width, height)
        } else if (root != null) {
            scene = new javafx.scene.Scene(root)
        } else {
            // allow empty scene, root can be attached later by onNodeChildren
            scene = new javafx.scene.Scene(new javafx.scene.Group())
        }

        // register variable after construction (legacy GroovyFX behavior)
        if (varId != null) {
            builder.setVariable(varId.toString(), scene)
        }

        return scene
    }


    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Parent) {
            // If a Parent is nested directly under scene, it becomes the root
            parent.root = (Parent) child
            return
        }

        if (child instanceof Node) {
            // If root is mutable children container, add it
            def root = parent.root
            if (root instanceof Pane) {
                root.children.add((Node) child)
                return
            }
            if (root instanceof Group) {
                root.children.add((Node) child)
                return
            }

            // Otherwise ignore (or you could throw)
            return
        }

        if (child instanceof StylesheetRef) {
            parent.stylesheets.add(child.url)
            return
        }

        if (child instanceof String) {
            if (looksLikeStylesheet(child)) {
                parent.stylesheets.add(child)
                return
            }
        }

        super.setChild(builder, parent, child)
    }

    private static double toDouble(Object v) {
        if (v == null) return -1d
        if (v instanceof Number) return ((Number) v).doubleValue()
        try {
            return Double.parseDouble(v.toString())
        } catch (ignored) {
            return -1d
        }
    }

    private static boolean looksLikeStylesheet(String s) {
        def v = s?.toLowerCase()
        return v?.endsWith(".css") || v?.startsWith("data:text/css") || v?.startsWith("http://") || v?.startsWith("https://")
    }
}
