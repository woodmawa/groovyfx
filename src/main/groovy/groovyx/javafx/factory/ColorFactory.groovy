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

import groovy.transform.CompileStatic
import groovyx.javafx.binding.BindingHolder
import javafx.beans.value.ObservableValue
import javafx.css.CssParser
import javafx.css.Stylesheet
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Paint
import javafx.scene.paint.RadialGradient
import javafx.scene.paint.Stop

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 * @author jimclarke
 * minor adaptions by hackergarten
 *
 * Patched for JavaFX 25+:
 * - unwrap groovyx.javafx.binding.BindingHolder so canvas operations (fill/stroke/etc)
 *   can accept bind { ... } expressions without feeding BindingHolder.toString() into CssParser.
 *   Key fix: unwrap Map / BindingHolder / ObservableValue so canvas ops like:
 *   fill(p: bind(...)) or fill(p: someBindingHolder)
 * don't stringify to "[p:BindingHolder@...]" and blow up parsing.
 */
class ColorFactory {

    // Accept: "#RRGGBB", "#AARRGGBB", "0xRRGGBB", "0xAARRGGBB", "red", "cornflowerblue", etc.
    // Also supports "rgb(r,g,b)" and "rgba(r,g,b,a)" (a as 0..1 or 0..255).
    private static final Pattern RGB_FUNC = Pattern.compile(/(?i)^\s*rgba?\s*\(\s*([^)]+)\s*\)\s*$/)

    static Paint get(Object value) {
        // ---- Unwrap the common wrapper shapes FIRST ----
        value = unwrap(value)

        if (value == null) return null

        if (value instanceof Paint) {
            return (Paint) value
        }

        if (value instanceof List || value instanceof Object[]) {
            Object[] args
            if (value instanceof List) {
                args = ((List) value).toArray()
            } else {
                args = (Object[]) value
            }
            return fromArgs(args)
        }

        // Anything else: treat as string (named color, web color, rgb(...), etc.)
        return fromString(String.valueOf(value))
    }

    /**
     * Unwraps Map / BindingHolder / ObservableValue recursively.
     */
    private static Object unwrap(Object v) {
        if (v == null) return null

        // Map forms coming from builder attributes, e.g. [p: BindingHolder(...)]
        if (v instanceof Map) {
            Map m = (Map) v
            if (m.containsKey('p')) return unwrap(m.get('p'))
            if (m.containsKey('paint')) return unwrap(m.get('paint'))
            if (m.containsKey('color')) return unwrap(m.get('color'))
            if (m.containsKey('value')) return unwrap(m.get('value'))
            // fall back: if it’s a single-entry map, unwrap the only value
            if (m.size() == 1) return unwrap(m.values().iterator().next())
            return v
        }

        // BindingHolder from groovyx.javafx.binding
        if (v instanceof BindingHolder) {
            BindingHolder bh = (BindingHolder) v
            // Common shapes: bh.value, bh.binding, bh.observable, etc. (varies across forks)
            // We try a few safe unwrapping options without assuming one exact API.
            def candidate = null
            try { candidate = bh.hasProperty('value') ? bh.getProperty('value') : null } catch (ignored) {}
            if (candidate != null) return unwrap(candidate)

            try { candidate = bh.hasProperty('binding') ? bh.getProperty('binding') : null } catch (ignored) {}
            if (candidate != null) return unwrap(candidate)

            try { candidate = bh.hasProperty('observable') ? bh.getProperty('observable') : null } catch (ignored) {}
            if (candidate != null) return unwrap(candidate)

            // last resort: leave as-is (caller may handle)
            return v
        }

        // ObservableValue (JavaFX property/binding)
        if (v instanceof ObservableValue) {
            return unwrap(((ObservableValue) v).value)
        }

        return v
    }

    private static Paint fromArgs(Object[] args) {
        if (args == null || args.length == 0) return null

        // Common: ["rgb", r, g, b] or ["rgba", r, g, b, a]
        if (args[0] instanceof String) {
            String cmd = ((String) args[0]).trim()
            if (cmd.equalsIgnoreCase("rgb")) {
                if (args.length < 4) throw new IllegalArgumentException("rgb requires 3 components")
                int r = toInt(args[1])
                int g = toInt(args[2])
                int b = toInt(args[3])
                return Color.rgb(clamp255(r), clamp255(g), clamp255(b))
            }
            if (cmd.equalsIgnoreCase("rgba")) {
                if (args.length < 5) throw new IllegalArgumentException("rgba requires 4 components")
                int r = toInt(args[1])
                int g = toInt(args[2])
                int b = toInt(args[3])
                double a = toAlpha(args[4])
                return Color.rgb(clamp255(r), clamp255(g), clamp255(b), clamp01(a))
            }
        }

        // Also allow: [r,g,b] or [r,g,b,a]
        if (args.length == 3 || args.length == 4) {
            int r = toInt(args[0])
            int g = toInt(args[1])
            int b = toInt(args[2])
            if (args.length == 3) {
                return Color.rgb(clamp255(r), clamp255(g), clamp255(b))
            } else {
                double a = toAlpha(args[3])
                return Color.rgb(clamp255(r), clamp255(g), clamp255(b), clamp01(a))
            }
        }

        // Fallback: if it’s a single thing, try parse it
        if (args.length == 1) {
            return get(args[0])
        }

        throw new IllegalArgumentException("Invalid color args: ${args.toList()}")
    }

    private static Paint fromString(String s) {
        if (s == null) return null
        String str = s.trim()
        if (str.isEmpty()) return null

        // rgb(...) / rgba(...)
        def m = RGB_FUNC.matcher(str)
        if (m.matches()) {
            String inner = m.group(1)
            List<String> parts = inner.split(/\s*,\s*/).toList()
            boolean rgba = str.toLowerCase().startsWith("rgba")
            if ((!rgba && parts.size() != 3) || (rgba && parts.size() != 4)) {
                throw new IllegalArgumentException("Invalid rgb/rgba syntax: '${str}'")
            }
            int r = toInt(parts[0])
            int g = toInt(parts[1])
            int b = toInt(parts[2])
            if (!rgba) {
                return Color.rgb(clamp255(r), clamp255(g), clamp255(b))
            } else {
                double a = toAlpha(parts[3])
                return Color.rgb(clamp255(r), clamp255(g), clamp255(b), clamp01(a))
            }
        }

        // JavaFX handles named colors, #hex, 0xhex, etc.
        try {
            return Color.web(str)
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid fill syntax: '${str}'", ex)
        }
    }

    private static int toInt(Object o) {
        if (o == null) return 0
        if (o instanceof Number) return ((Number) o).intValue()
        return Integer.parseInt(String.valueOf(o).trim())
    }

    /**
     * Accept alpha as:
     * - 0..1 (float/double)
     * - 0..255 (int)
     * - string forms
     */
    private static double toAlpha(Object o) {
        if (o == null) return 1.0d
        if (o instanceof Number) {
            double d = ((Number) o).doubleValue()
            // Heuristic: if > 1 treat as 0..255
            if (d > 1.0d) return d / 255.0d
            return d
        }
        String s = String.valueOf(o).trim()
        if (s.isEmpty()) return 1.0d
        double d = Double.parseDouble(s)
        if (d > 1.0d) return d / 255.0d
        return d
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v))
    }

    private static double clamp01(double v) {
        return Math.max(0.0d, Math.min(1.0d, v))
    }
}