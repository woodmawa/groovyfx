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
import groovy.util.AbstractFactory
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/**
 * Modern JavaFX LinearGradient factory (no deprecated Builder API).
 *
 * Supported attributes:
 *  - start: [x, y] (default [0,0])
 *  - end:   [x, y] (default [1,0])
 *  - proportional: boolean (default true)
 *  - cycleMethod:  'noCycle'|'reflect'|'repeat' or CycleMethod (default NO_CYCLE)
 *  - stops:  one of:
 *      * List<String|Color>                   => evenly distributed offsets
 *      * List<[offset, color]>                => explicit offsets (0..1)
 *      * List<Map(offset:..., color:...)>     => explicit offsets
 *      * List<Stop>                           => used as-is
 *
 * Also supports nested stop(...) child nodes if you already have a StopFactory:
 *   linearGradient { stop(offset:0.0, color:'#fff'); stop(offset:1.0, color:'#000') }
 */
class LinearGradientFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (value instanceof LinearGradient) return value

        def start = normalizePoint(attributes.remove('start') ?: [0d, 0d])
        def end   = normalizePoint(attributes.remove('end')   ?: [1d, 0d])

        boolean proportional = attributes.containsKey('proportional')
                ? (attributes.remove('proportional') as boolean)
                : true

        CycleMethod cycleMethod = attributes.containsKey('cycleMethod')
                ? toCycleMethod(attributes.remove('cycleMethod'))
                : CycleMethod.NO_CYCLE

        def stopsSpec = attributes.remove('stops') ?: attributes.remove('stop') ?: attributes.remove('colors')

        // If stops are provided via attributes, build them now.
        // If not, we’ll allow child stop nodes to populate via setChild().
        List<Stop> stops = []
        if (stopsSpec != null) {
            stops = toStops(stopsSpec)
        }

        if (!attributes.isEmpty()) {
            throw new IllegalArgumentException("Unsupported linearGradient attributes: ${attributes.keySet().sort()}")
        }

        return new LinearGradient(
                start[0], start[1],
                end[0], end[1],
                proportional,
                cycleMethod,
                stops
        )
    }

    @Override
    boolean isLeaf() {
        // Not strictly a “leaf” if you want to allow nested stop nodes.
        // Return false so FactoryBuilderSupport can call setChild().
        return false
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof LinearGradient && child instanceof Stop) {
            // LinearGradient is immutable, so we can't "add" to it directly.
            // Strategy: collect stops on builder context, then rebuild in onNodeCompleted.
            def ctx = builder.getContext()
            (ctx._linearGradientStops ?: (ctx._linearGradientStops = [])).add(child)
            return
        }
        throw new IllegalArgumentException("linearGradient only accepts Stop children, got: ${child?.getClass()?.name}")
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // If child Stop nodes were used, rebuild gradient with them.
        def ctx = builder.getContext()
        def collected = (ctx?._linearGradientStops as List<Stop>)
        if (node instanceof LinearGradient && collected && !collected.isEmpty()) {
            // Rebuild with collected stops; preserve all other properties:
            def g = (LinearGradient) node
            def rebuilt = new LinearGradient(
                    g.startX, g.startY,
                    g.endX, g.endY,
                    g.proportional,
                    g.cycleMethod,
                    collected
            )
            // Replace the node in its parent if needed:
            builder.setProperty(parent, builder.getCurrentName(), rebuilt)
            ctx._linearGradientStops = null
        }
    }

    private static List<Double> normalizePoint(def p) {
        if (p instanceof List && p.size() == 2) {
            return [(p[0] as double), (p[1] as double)]
        }
        if (p instanceof Number[]) {
            def a = p as Number[]
            if (a.length == 2) return [(a[0] as double), (a[1] as double)]
        }
        throw new IllegalArgumentException("start/end must be [x, y], got: ${p}")
    }

    private static CycleMethod toCycleMethod(def v) {
        if (v instanceof CycleMethod) return (CycleMethod) v
        def s = v?.toString()?.trim()?.toLowerCase()
        switch (s) {
            case 'nocycle':
            case 'no_cycle':
            case 'no-cycle':
            case 'none':
                return CycleMethod.NO_CYCLE
            case 'reflect':
                return CycleMethod.REFLECT
            case 'repeat':
                return CycleMethod.REPEAT
        }
        throw new IllegalArgumentException("Unknown cycleMethod: ${v} (use noCycle|reflect|repeat)")
    }

    private static List<Stop> toStops(def stopsSpec) {
        if (!(stopsSpec instanceof List)) {
            throw new IllegalArgumentException("stops must be a List, got: ${stopsSpec?.getClass()?.name}")
        }

        def list = (List) stopsSpec
        if (list.isEmpty()) return []

        // Already Stops?
        if (list.every { it instanceof Stop }) {
            return list as List<Stop>
        }

        // Colors-only? (String/Color)
        if (list.every { it instanceof String || it instanceof Color }) {
            return distributeEvenly(list.collect { toColor(it) })
        }

        // Tuples [offset, color]
        if (list.every { it instanceof List && ((List) it).size() == 2 }) {
            return list.collect { pair ->
                def p = (List) pair
                new Stop((p[0] as double), toColor(p[1]))
            }
        }

        // Maps [offset:..., color:...]
        if (list.every { it instanceof Map }) {
            return list.collect { m ->
                def mm = (Map) m
                if (!mm.containsKey('offset') || !mm.containsKey('color')) {
                    throw new IllegalArgumentException("Stop map must contain offset and color. Got: ${mm}")
                }
                new Stop((mm.offset as double), toColor(mm.color))
            }
        }

        throw new IllegalArgumentException("Unsupported stops format: ${list}")
    }

    private static List<Stop> distributeEvenly(List<Color> colors) {
        int n = colors.size()
        if (n == 1) return [new Stop(0d, colors[0])]

        def stops = []
        for (int i = 0; i < n; i++) {
            double offset = (double) i / (double) (n - 1)
            stops << new Stop(offset, colors[i])
        }
        return stops
    }

    private static Color toColor(def c) {
        if (c instanceof Color) return (Color) c
        if (c instanceof String) {
            // Handles "#rrggbb", "#rrggbbaa", and many CSS color forms
            return Color.web((String) c)
        }
        throw new IllegalArgumentException("Stop color must be String or Color, got: ${c?.getClass()?.name}")
    }
}
