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
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Paint

/**
 * SceneFactory - creates a JavaFX Scene and sets its root.
 *
 * Compatibility notes:
 * - JavaFX Scene root must be a Parent.
 * - Many demos put a plain Node directly under scene { ... } (ImageView, MediaView, Shapes, etc).
 * - A Group root "works" for parenting but provides no layout/sizing; MediaView in particular
 *   commonly needs a layout container to be visible without extra boilerplate.
 *
 * Therefore: when no explicit root is provided, we default the root to a StackPane.
 * This preserves the GroovyFX DSL style (scene { node() }) while producing visible results
 * across modern JavaFX versions.
 *
 * Supports:
 * - scene(width:..., height:..., fill:..., depthBuffer:...) { ... }
 * - nested nodes added to a default root container
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

        // Determine root from value or attributes
        Parent root = null
        if (value instanceof Parent) {
            root = (Parent) value
        } else if (attributes.containsKey('root')) {
            def r = attributes.remove('root')
            if (r instanceof Parent) {
                root = (Parent) r
            } else if (r != null) {
                throw new IllegalArgumentException("scene(root: ...) must be a javafx.scene.Parent, got: ${r.getClass().name}")
            }
        }

        double width  = attributes.containsKey('width')  ? (attributes.remove('width')  as Number).doubleValue()  : 0d
        double height = attributes.containsKey('height') ? (attributes.remove('height') as Number).doubleValue() : 0d

        // Scene.fill is NOT read-only, but we must ensure it is applied even when width/height are omitted.
        def fill = attributes.remove('fill') // may be Color/Paint/etc (factories/coercion handle this)

        // depthBuffer is read-only post-construction; must be a ctor arg if provided.
        boolean depthBuffer = false
        if (attributes.containsKey('depthBuffer')) {
            def db = attributes.remove('depthBuffer')
            depthBuffer = (db instanceof Boolean) ? (boolean) db : (db != null ? db.toString().toBoolean() : false)
        }

        // If no root supplied, use a layout container root (StackPane) rather than Group.
        if (root == null) {
            root = new StackPane()
        }

        javafx.scene.Scene scene

        // Prefer constructors that avoid touching read-only properties later.
        if (width > 0 && height > 0) {
            if (depthBuffer) {
                // Use the depthBuffer ctor, then apply fill via setter if present.
                scene = new javafx.scene.Scene(root, width, height, true)
            } else {
                scene = new javafx.scene.Scene(root, width, height)
            }
        } else {
            // No explicit size.
            scene = new javafx.scene.Scene(root)
            // depthBuffer cannot be enabled after construction if no size provided.
            // (If you need depthBuffer with no size, pass width/height in DSL.)
        }

        // Ensure fill is always applied (even when not passed via ctor).
        if (fill != null) {
            scene.setFill((Paint) fill)
        }

        if (varId != null) {
            builder.setVariable(varId.toString(), scene)
        }

        return scene
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // A Parent nested directly under scene becomes the root
        if (child instanceof Parent) {
            parent.root = (Parent) child
            return
        }

        // Plain Node: add to root if it has a children list
        if (child instanceof Node) {
            def root = parent.root

            // Most common containers
            if (root instanceof Pane) {
                root.children.add((Node) child)
                return
            }
            if (root instanceof Group) {
                root.children.add((Node) child)
                return
            }

            // Default root we create is StackPane (also a Pane), but keep this for clarity/defense
            if (root instanceof StackPane) {
                root.children.add((Node) child)
                return
            }

            // If root is some other Parent with no children list, do nothing
            // (or could throw, but "ignore" matches legacy behavior)
            return
        }

        // Stylesheets
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

    private static boolean looksLikeStylesheet(String s) {
        def v = s?.toLowerCase()
        return v?.endsWith(".css") ||
                v?.startsWith("data:text/css") ||
                v?.startsWith("http://") ||
                v?.startsWith("https://")
    }
}
