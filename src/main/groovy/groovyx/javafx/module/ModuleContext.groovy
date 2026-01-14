package groovyx.javafx.module

import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * A light wrapper around a Map used as the UI build/invocation context.
 *
 * Backwards compatible: implements Map via delegation, so existing signatures
 * like UIModule.build(Map ctx) continue to work when passed a ModuleContext.
 */
@ToString(includeNames = true, includePackage = false)
@CompileStatic
class ModuleContext implements Map<String, Object> {

    @Delegate
    private final Map<String, Object> data

    ModuleContext() {
        this.data = new LinkedHashMap<>()
    }

    ModuleContext(Map<String, Object> data) {
        this.data = new LinkedHashMap<String, Object>()
        if (data != null) this.data.putAll(data)
    }

    /** Convenience: wrap an existing Map or return the ModuleContext as-is. */
    static ModuleContext of(Object ctx) {
        if (ctx instanceof ModuleContext) return (ModuleContext) ctx
        if (ctx instanceof Map) return new ModuleContext((Map<String, Object>) ctx)
        return new ModuleContext()
    }

    Map<String, Object> getHandlers() {
        (Map<String, Object>) data.computeIfAbsent("handlers") { new LinkedHashMap<>() }
    }

    Map<String, Object> getServices() {
        (Map<String, Object>) data.computeIfAbsent("services") { new LinkedHashMap<>() }
    }

    Map<String, Object> getState() {
        (Map<String, Object>) data.computeIfAbsent("state") { new LinkedHashMap<>() }
    }

    Map<String, Object> asMap() {
        data
    }

    //convenience access
    Object getAttr(String name) {
        data.get(name)
    }

    void setAttr(String name, Object value) {
        data.put(name, value)
    }
}
