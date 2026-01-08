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

import groovy.util.AbstractFactory
import groovy.util.FactoryBuilderSupport
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.RadialGradient
import javafx.scene.paint.Stop
import javafx.scene.paint.Color

/**
 * JavaFX 8+ compatible RadialGradient factory.
 *
 * Supports:
 *   radialGradient(focusAngle:..., focusDistance:..., centerX:..., centerY:..., radius:..., proportional:..., cycleMethod:...) {
 *       stop(offset: 0.0, color: Color.WHITE)
 *       stop(offset: 1.0, color: Color.BLACK)
 *   }
 *
 * Also supports:
 *   radialGradient(stops: [new Stop(...), ...], ...)
 */
class RadialGradientFactory extends AbstractFactory {

    static class Spec {
        double focusAngle = 0d
        double focusDistance = 0d
        double centerX = 0d
        double centerY = 0d
        double radius = 1d
        boolean proportional = true
        CycleMethod cycleMethod = CycleMethod.NO_CYCLE
        final List<Stop> stops = []

        Object build() {
            // JavaFX requires stops; provide a harmless default if none were supplied
            List<Stop> finalStops = stops
            if (finalStops == null || finalStops.isEmpty()) {
                finalStops = [
                        new Stop(0d, Color.TRANSPARENT),
                        new Stop(1d, Color.TRANSPARENT)
                ]
            }
            return new RadialGradient(
                    focusAngle,
                    focusDistance,
                    centerX,
                    centerY,
                    radius,
                    proportional,
                    cycleMethod,
                    finalStops
            )
        }
    }

    @Override
    boolean isLeaf() { false }  // we accept stop children

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        def spec = new Spec()

        // Allow passing an existing RadialGradient directly
        if (value instanceof RadialGradient) return value

        // Map attributes -> spec fields
        if (attributes.containsKey('focusAngle'))     spec.focusAngle = toDouble(attributes.remove('focusAngle'))
        if (attributes.containsKey('focusDistance'))  spec.focusDistance = toDouble(attributes.remove('focusDistance'))

        if (attributes.containsKey('centerX'))        spec.centerX = toDouble(attributes.remove('centerX'))
        if (attributes.containsKey('centerY'))        spec.centerY = toDouble(attributes.remove('centerY'))

        // aliases: cx / cy
        if (attributes.containsKey('cx'))             spec.centerX = toDouble(attributes.remove('cx'))
        if (attributes.containsKey('cy'))             spec.centerY = toDouble(attributes.remove('cy'))

        if (attributes.containsKey('radius'))         spec.radius = toDouble(attributes.remove('radius'))
        if (attributes.containsKey('proportional'))   spec.proportional = toBoolean(attributes.remove('proportional'))

        if (attributes.containsKey('cycleMethod')) {
            def cm = attributes.remove('cycleMethod')
            spec.cycleMethod = toCycleMethod(cm)
        }

        // Stops may be passed as attribute or value (List/array)
        def stopsAttr = attributes.remove('stops')
        def candidate = (value != null) ? value : stopsAttr
        addStops(spec, candidate)

        // Back-compat alias: center: [x, y]  (or Point2D / Map)
        if (attributes.containsKey('center')) {
            def c = attributes.remove('center')

            if (c instanceof javafx.geometry.Point2D) {
                spec.centerX = ((javafx.geometry.Point2D) c).x
                spec.centerY = ((javafx.geometry.Point2D) c).y
            } else if (c instanceof Map) {
                // accept [x:.., y:..] or [centerX:.., centerY:..]
                def mx = (c.x != null) ? c.x : c.centerX
                def my = (c.y != null) ? c.y : c.centerY
                if (mx != null) spec.centerX = toDouble(mx)
                if (my != null) spec.centerY = toDouble(my)
            } else if (c instanceof List || c instanceof Object[]) {
                def list = (c instanceof List) ? (List) c : (c as Object[]).toList()
                if (list.size() >= 2) {
                    spec.centerX = toDouble(list[0])
                    spec.centerY = toDouble(list[1])
                }
            } else {
                // last-ditch: allow "x,y"
                def s = c?.toString()
                if (s?.contains(',')) {
                    def parts = s.split(',').collect { it.trim() }
                    if (parts.size() >= 2) {
                        spec.centerX = toDouble(parts[0])
                        spec.centerY = toDouble(parts[1])
                    }
                }
            }
        }

        return spec
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // If someone passes a raw RadialGradient, just ignore children
        if (parent instanceof RadialGradient) return

        if (parent instanceof Spec) {
            addStops(parent as Spec, child)
        }
    }

    private static void addStops(Spec spec, Object candidate) {
        if (candidate == null) return

        if (candidate instanceof Stop) {
            spec.stops << (Stop) candidate
            return
        }

        if (candidate instanceof Stop[]) {
            spec.stops.addAll(((Stop[])candidate).toList())
            return
        }

        if (candidate instanceof Iterable) {
            (candidate as Iterable).each { spec.stops << (it as Stop) }
            return
        }
    }

    private static double toDouble(Object v) {
        if (v == null) return 0d
        if (v instanceof Number) return ((Number)v).doubleValue()
        return Double.parseDouble(v.toString())
    }

    private static boolean toBoolean(Object v) {
        if (v == null) return false
        if (v instanceof Boolean) return (Boolean) v
        return v.toString().toBoolean()
    }

    private static CycleMethod toCycleMethod(Object v) {
        if (v instanceof CycleMethod) return (CycleMethod) v
        if (v == null) return CycleMethod.NO_CYCLE
        return CycleMethod.valueOf(v.toString().trim().toUpperCase())
    }
}