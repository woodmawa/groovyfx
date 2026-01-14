package groovyx.javafx.module

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Simple module registry for runtime reuse.
 * Safe defaults:
 * - stores module *definitions* (not Nodes)
 * - returns modules by name
 */
final class ModuleRegistry {
    private static final ConcurrentMap<String, UIModule> modules = new ConcurrentHashMap<>()

    ModuleRegistry() {}

    static void register(String name, UIModule module) {
        assert name?.trim()
        assert module != null
        modules.put(name, module)
    }

    static UIModule get(String name) {
        assert name?.trim()
        def m = modules.get(name)
        if (m == null) {
            throw new IllegalStateException("No UIModule registered for name='${name}'")
        }
        return m
    }

    /**
     * Optional: dev helpers
     */
    static boolean has(String name) {
        assert name?.trim()
        return modules.containsKey(name)
    }

    static UIModule remove(String name) {
        assert name?.trim()
        return modules.remove(name)
    }

    static void clear() {
        modules.clear()
    }

    static Map<String, UIModule> snapshot() {
        // stable view for debugging/tools
        return Collections.unmodifiableMap(new LinkedHashMap<>(modules))
    }
}
