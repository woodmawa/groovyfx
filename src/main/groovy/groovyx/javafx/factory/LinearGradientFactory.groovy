/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011-2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package groovyx.javafx.factory

import groovy.util.AbstractFactory
import groovy.util.FactoryBuilderSupport
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/**
 * JavaFX LinearGradient factory (no deprecated Builder API).
 *
 * Supported attributes:
 *  - start: [x, y] (default [0,0])
 *  - end:   [x, y] (default [1,0])
 *  - startX/startY/endX/endY: numeric shorthands (legacy GroovyFX convenience)
 *  - proportional: boolean (default true)
 *  - cycleMethod:  'noCycle'|'reflect'|'repeat' or CycleMethod (default NO_CYCLE)
 *  - stops/stop/colors:
 *      * Map<Number, Color|String>             => explicit offsets (e.g. [0.25: YELLOW, 1.0: BLUE])
 *      * List<String|Color>                    => evenly distributed offsets
 *      * List<[offset, color]>                 => explicit offsets
 *      * List<Map(offset:..., color:...)>      => explicit offsets
 *      * List<Stop>                            => used as-is
 *
 * Also supports nested stop(.) child nodes.
 */
class LinearGradientFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (value instanceof LinearGradient) return value

        // --- legacy point shorthands (startX/startY/endX/endY) ---
        double startX = popDouble(attributes, 'startX', 0d)
        double startY = popDouble(attributes, 'startY', 0d)
        double endX   = popDouble(attributes, 'endX',   1d)
        double endY   = popDouble(attributes, 'endY',   0d)

        // --- modern point pairs override shorthands if present ---
        def startPair = attributes.remove('start')
        def endPair   = attributes.remove('end')
        if (startPair != null) {
            def p = normalizePoint(startPair)
            startX = p[0]; startY = p[1]
        }
        if (endPair != null) {
            def p = normalizePoint(endPair)
            endX = p[0]; endY = p[1]
        }

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
                startX, startY,
                endX, endY,
                proportional,
                cycleMethod,
                stops
        )
    }

    @Override
    boolean isLeaf() {
        // allow nested stop nodes
        return false
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof LinearGradient && child instanceof Stop) {
            def ctx = builder.getContext()
            (ctx._linearGradientStops ?: (ctx._linearGradientStops = [])).add(child)
            return
        }
        throw new IllegalArgumentException("linearGradient only accepts Stop children, got: ${child?.getClass()?.name}")
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        def ctx = builder.getContext()
        def collected = (ctx?._linearGradientStops as List<Stop>)
        if (node instanceof LinearGradient && collected && !collected.isEmpty()) {
            def g = (LinearGradient) node
            def rebuilt = new LinearGradient(
                    g.startX, g.startY,
                    g.endX, g.endY,
                    g.proportional,
                    g.cycleMethod,
                    collected
            )
            builder.setProperty(parent, builder.getCurrentName(), rebuilt)
            ctx._linearGradientStops = null
        }
    }

    private static double popDouble(Map attrs, String key, double defaultValue) {
        if (!attrs.containsKey(key)) return defaultValue
        def v = attrs.remove(key)
        if (v == null) return defaultValue
        if (v instanceof Number) return ((Number) v).doubleValue()
        return (v as double)
    }

    private static List<Double> normalizePoint(def p) {
        if (p instanceof List && ((List) p).size() == 2) {
            def l = (List) p
            return [(l[0] as double), (l[1] as double)]
        }
        if (p instanceof Number[]) {
            def a = (Number[]) p
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
        // --- NEW: allow Map<offset,color> directly ---
        if (stopsSpec instanceof Map) {
            def m = (Map) stopsSpec
            if (m.isEmpty()) return []
            return m.entrySet()
                    .collect { e ->
                        def k = e.key
                        if (!(k instanceof Number)) {
                            throw new IllegalArgumentException("Stop map keys must be numbers (offsets). Got: ${k?.getClass()?.name}")
                        }
                        new Stop(((Number) k).doubleValue(), toColor(e.value))
                    }
                    .sort { a, b -> a.offset <=> b.offset }
        }

        if (!(stopsSpec instanceof List)) {
            throw new IllegalArgumentException("stops must be a List or Map, got: ${stopsSpec?.getClass()?.name}")
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
            return list.collect { mm ->
                def m = (Map) mm
                if (!m.containsKey('offset') || !m.containsKey('color')) {
                    throw new IllegalArgumentException("Stop map must contain offset and color. Got: ${m}")
                }
                new Stop((m.offset as double), toColor(m.color))
            }
        }

        throw new IllegalArgumentException("Unsupported stops format: ${stopsSpec}")
    }

    private static List<Stop> distributeEvenly(List<Color> colors) {
        if (colors.isEmpty()) return []
        if (colors.size() == 1) return [new Stop(0d, colors[0]), new Stop(1d, colors[0])]
        int n = colors.size()
        return (0..<n).collect { i ->
            double offset = (double) i / (double) (n - 1)
            new Stop(offset, colors[i])
        }
    }

    private static Color toColor(def v) {
        if (v instanceof Color) return (Color) v
        if (v == null) throw new IllegalArgumentException("Color value cannot be null")
        return Color.web(v.toString())
    }
}
