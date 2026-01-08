package groovyx.javafx.canvas

import javafx.beans.value.ObservableValue

trait OpParamCoercion {

    /** Extract value from scalar or a map with expected key(s). */
    Object pick(Object params, List<String> keys) {
        if (params instanceof Map) {
            Map m = (Map) params
            for (k in keys) {
                if (m.containsKey(k)) return m.get(k)
            }
            // fallback: if map has exactly one entry, assume it's the value
            if (m.size() == 1) return m.values().iterator().next()
            return null
        }
        return params
    }

    /** Best-effort "binding unwrap" (BindingHolder / Closure / ObservableValue, etc.). */
    Object unwrap(Object v) {
        if (v == null) return null

        // Keep unwrapping until stable (prevents holder->value->Observable->value chains)
        Object cur = v
        while (true) {
            if (cur == null) return null

            // 1) GroovyFX bind { ... } returns a BindingHolder
            //    We avoid a hard compile-time dependency by checking class name.
            if (cur.getClass().name == 'groovyx.javafx.binding.BindingHolder') {
                def inner = null
                def mc = cur.metaClass

                try {
                    // Common BindingHolder shapes (depending on GroovyFX version)
                    if (mc.respondsTo(cur, 'getValue')) {
                        inner = cur.getValue()
                    } else if (mc.hasProperty(cur, 'value')) {
                        inner = cur.value
                    } else if (mc.hasProperty(cur, 'binding')) {
                        inner = cur.binding
                    } else if (mc.hasProperty(cur, 'closure')) {
                        inner = cur.closure
                    }
                } catch (Throwable ignored) {
                    inner = null
                }

                // If holder wraps a closure, evaluate it
                if (inner instanceof Closure) {
                    inner = (inner as Closure).call()
                }

                // continue unwrapping whatever we found
                cur = inner
                continue
            }

            // 2) JavaFX ObservableValue (Property/Binding/etc.)
            if (cur instanceof ObservableValue) {
                cur = (cur as ObservableValue).getValue()
                continue
            }

            // 3) Lazy closures (builder sometimes passes Closure directly)
            if (cur instanceof Closure) {
                cur = (cur as Closure).call()
                continue
            }

            // 4) Generic "holder" patterns: getValue()/value (but don't loop forever)
            def mc = cur.metaClass
            if (mc.respondsTo(cur, 'getValue')) {
                def next = cur.getValue()
                if (!java.util.Objects.equals(next, cur)) {
                    cur = next
                    continue
                }
            }
            if (mc.hasProperty(cur, 'value')) {
                def next = cur.value
                if (!java.util.Objects.equals(next, cur)) {
                    cur = next
                    continue
                }
            }

            // Stable value reached
            return cur
        }
    }

    /** Safe-ish coercion to Double/Float/Integer/etc. */
    def <T> T coerce(Object v, Class<T> target) {
        v = unwrap(v)
        if (v == null) return null

        if (target == Double) {
            if (v instanceof Number) return (T) ((Number) v).doubleValue()
            return (T) Double.valueOf(v.toString())
        }
        if (target == Float) {
            if (v instanceof Number) return (T) ((Number) v).floatValue()
            return (T) Float.valueOf(v.toString())
        }
        if (target == Integer) {
            if (v instanceof Number) return (T) ((Number) v).intValue()
            return (T) Integer.valueOf(v.toString())
        }

        // Key fix: variable Class<T> needs asType(Class), not `as <type literal>`
        return (T) v.asType(target)
    }
}
