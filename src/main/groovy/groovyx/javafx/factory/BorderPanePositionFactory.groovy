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
import groovy.util.FactoryBuilderSupport
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node

/**
 * Creates a BorderPanePosition wrapper node for BorderPane regions.
 *
 * Supports both:
 *   new BorderPanePositionFactory()           // region inferred from node name or attributes
 *   new BorderPanePositionFactory("top")      // region fixed by SceneGraphBuilder auto-registration
 *
 * Back-compat:
 *   top(align:CENTER, margin:10) { label("Top") }
 */
class BorderPanePositionFactory extends AbstractNodeFactory {

    private final String defaultRegion

    BorderPanePositionFactory() {
        super(BorderPanePosition)
        this.defaultRegion = null
    }

    /** Needed for SceneGraphBuilder.registerContainers() */
    BorderPanePositionFactory(String defaultRegion) {
        super(BorderPanePosition)
        this.defaultRegion = defaultRegion
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        // Region resolution priority:
        // 1) explicit attribute 'region'
        // 2) factory defaultRegion (when auto-registered for 'top','left',...)
        // 3) node name (e.g. "top")
        def region = attributes.remove("region")
        if (region == null) region = defaultRegion
        if (region == null) region = name?.toString()

        if (region == null) {
            throw new IllegalArgumentException("BorderPanePosition requires a region (top/bottom/left/right/center).")
        }

        def pos = new BorderPanePosition(region.toString(), null)

        // Back-compat: align:
        if (attributes.containsKey("align")) {
            pos.align = toPos(attributes.remove("align"))
        }

        // Back-compat: margin:
        if (attributes.containsKey("margin")) {
            pos.margin = toInsets(attributes.remove("margin"))
        }

        return pos
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (!(parent instanceof BorderPanePosition)) return
        if (child instanceof Node) parent.node = (Node) child
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // In some builder flows, the parent will be the BorderPane and the child is the position wrapper
        if (parent instanceof javafx.scene.layout.BorderPane && node instanceof BorderPanePosition) {
            node.applyTo((javafx.scene.layout.BorderPane) parent)
        }
    }

    private static Pos toPos(Object v) {
        if (v == null) return null
        if (v instanceof Pos) return (Pos) v

        def s = v.toString().trim()
        if (!s) return null

        s = s.replace('-', '_')
                .replace(' ', '_')
                .toUpperCase()

        try {
            return Pos.valueOf(s)
        } catch (Throwable t) {
            throw new IllegalArgumentException(
                    "Invalid align value '$v' (expected javafx.geometry.Pos or a valid Pos name like CENTER, TOP_RIGHT)."
            )
        }
    }

    private static Insets toInsets(Object v) {
        if (v == null) return null
        if (v instanceof Insets) return (Insets) v

        if (v instanceof Number) {
            double d = ((Number) v).doubleValue()
            return new Insets(d, d, d, d)
        }

        if (v instanceof List || v instanceof Object[]) {
            def list = (v instanceof List) ? (List) v : (v as Object[]).toList()
            def nums = list.collect { it == null ? null : (it as Number).doubleValue() }
            if (nums.size() == 1) return new Insets(nums[0], nums[0], nums[0], nums[0])
            if (nums.size() == 2) return new Insets(nums[0], nums[1], nums[0], nums[1])
            if (nums.size() == 3) return new Insets(nums[0], nums[1], nums[2], nums[1])
            if (nums.size() >= 4) return new Insets(nums[0], nums[1], nums[2], nums[3])
        }

        if (v instanceof Map) {
            def m = (Map) v
            def top = pickNum(m, 'top', 't')
            def right = pickNum(m, 'right', 'r')
            def bottom = pickNum(m, 'bottom', 'b')
            def left = pickNum(m, 'left', 'l')

            def provided = [top, right, bottom, left].findAll { it != null }
            if (provided.size() == 1) {
                def d = provided[0]
                return new Insets(d, d, d, d)
            }
            return new Insets(top ?: 0d, right ?: 0d, bottom ?: 0d, left ?: 0d)
        }

        def s = v.toString().trim()
        if (!s) return null
        if (!s.contains(',')) {
            def d = Double.parseDouble(s)
            return new Insets(d, d, d, d)
        }

        def parts = s.split(',').collect { it.trim() }.findAll { it.length() > 0 }
        def nums = parts.collect { Double.parseDouble(it) }
        if (nums.size() == 1) return new Insets(nums[0], nums[0], nums[0], nums[0])
        if (nums.size() == 2) return new Insets(nums[0], nums[1], nums[0], nums[1])
        if (nums.size() == 3) return new Insets(nums[0], nums[1], nums[2], nums[1])
        return new Insets(nums[0], nums[1], nums[2], nums[3])
    }

    private static Double pickNum(Map m, String k1, String k2) {
        def v = m.containsKey(k1) ? m[k1] : (m.containsKey(k2) ? m[k2] : null)
        return v == null ? null : ((Number) v).doubleValue()
    }
}