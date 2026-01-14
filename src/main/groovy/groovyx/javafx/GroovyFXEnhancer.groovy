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
package groovyx.javafx

import groovy.util.FactoryBuilderSupport
import groovy.util.logging.Slf4j
import javafx.scene.control.SplitPane
import org.codehaus.groovy.runtime.InvokerHelper

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Callback

/**
 * Restores legacy GroovyFX metaclass enhancements expected by factories.
 *
 * Key fixes:
 *  - Adds Object.onHandleNodeAttributes(builder, node, attrs) static hook
 *    used by AbstractNodeFactory.
 *  - Adds Object.setChild(builder, parent, child) static hook used by factories.
 *  - Adds Object.onNodeCompleted(builder, parent, node) static hook used by bindings/factories.
 *  - Chains existing methodMissing handlers instead of clobbering them.
 *  - Adds null-safe fallbacks for List/Map/Set asType interception.
 *
 * Hardenings / optional modules:
 *  - WebEngine enhancements are applied only if javafx.web is present.
 */
@Slf4j
class GroovyFXEnhancer {

    private static volatile boolean enhanced = false
    private static final Object LOCK = new Object()

    /** Capability probe cached once; avoids hard dependency on javafx.web. */
    private static final boolean HAS_JAVAFX_WEB = hasClass("javafx.scene.web.WebEngine")

    static void enhanceClasses() {
        if (enhanced) return
        synchronized (LOCK) {
            if (enhanced) return
            doEnhance()
            enhanced = true
        }
    }

    private static void doEnhance() {
        // ------------------------------------------------------------------
        // 1) REQUIRED by groovyx.javafx.factory.AbstractNodeFactory + bindings
        // ------------------------------------------------------------------
        ensureOnHandleNodeAttributesHook()
        ensureSetChildHook()
        ensureOnNodeCompletedHook()

        // ------------------------------------------------------------------
        // 2) Collection coercions
        // ------------------------------------------------------------------
        enhanceCollectionCoercions()

        // ------------------------------------------------------------------
        // 3) Shortcut: xxx() -> xxxProperty() for Node/Scene/Stage (CHAINED)
        // ------------------------------------------------------------------
        enhancePropertyShortcutMethodMissing()

        // ------------------------------------------------------------------
        // 4) OPTIONAL: WebEngine handler sugar (only if javafx.web is present)
        // ------------------------------------------------------------------
        enhanceWebEngineSugarIfPresent()
    }

    // ----------------------------------------------------------------------
    // Phase 1: static factory hooks expected by legacy factories/bindings
    // ----------------------------------------------------------------------

    private static void ensureOnHandleNodeAttributesHook() {
        def existing = Object.metaClass.getMetaMethod(
                "onHandleNodeAttributes",
                [FactoryBuilderSupport, Object, Map] as Class[]
        )
        if (existing != null) return

        Object.metaClass.'static'.onHandleNodeAttributes = { FactoryBuilderSupport builder, Object node, Map attrs ->
            // Must be safe when attrs is empty (common in tests)
            if (attrs == null || attrs.isEmpty()) return attrs
            // Conservative: do not consume; let factories handle it.
            return attrs
        }
    }

    private static void ensureSetChildHook() {
        def existing = Object.metaClass.getMetaMethod(
                "setChild",
                [FactoryBuilderSupport, Object, Object] as Class[]
        )
        if (existing != null) return

        Object.metaClass.'static'.setChild = { FactoryBuilderSupport builder, Object parent, Object child ->
            if (parent == null || child == null) return

            def effectiveChild = coerceChildIfBuildable(child)

            if (effectiveChild instanceof javafx.scene.Node) {
                // 1) graphic property
                if (trySetGraphicProperty(parent, (javafx.scene.Node) effectiveChild)) return
                // 2) setGraphic(Node)
                if (tryCallSetGraphic(parent, (javafx.scene.Node) effectiveChild)) return
                // 3) Parent.children
                if (tryAddToChildren(parent, (javafx.scene.Node) effectiveChild)) return
                // 4) SplitPane.items (Nodes only)
                if (tryAddToSplitPane(parent, (javafx.scene.Node) effectiveChild)) return
            }

            return
        }
    }

    private static Object coerceChildIfBuildable(Object child) {
        def effectiveChild = child
        try {
            if (!(child instanceof javafx.scene.Node) &&
                    child.metaClass.respondsTo(child, "build", InvokerHelper.EMPTY_ARGUMENTS)) {
                def built = child.build()
                if (built != null) effectiveChild = built
            }
        } catch (Throwable ignored) {
            // best effort; keep original child
        }
        return effectiveChild
    }

    private static boolean trySetGraphicProperty(Object parent, javafx.scene.Node child) {
        try {
            def p = parent.metaClass.hasProperty(parent, "graphic")
            if (p != null) {
                parent.graphic = child
                return true
            }
        } catch (Throwable ignored) { }
        return false
    }

    private static boolean tryCallSetGraphic(Object parent, javafx.scene.Node child) {
        try {
            if (parent.metaClass.respondsTo(parent, "setGraphic", javafx.scene.Node)) {
                parent.setGraphic(child)
                return true
            }
        } catch (Throwable ignored) { }
        return false
    }

    private static boolean tryAddToChildren(Object parent, javafx.scene.Node child) {
        try {
            def chProp = parent.metaClass.hasProperty(parent, "children")
            if (chProp != null && parent.children instanceof Collection) {
                parent.children.add(child)
                return true
            }
        } catch (Throwable ignored) { }
        return false
    }

    private static boolean tryAddToSplitPane(Object parent, javafx.scene.Node child) {
        try {
            if (parent instanceof SplitPane) {
                parent.items.add(child)
                return true
            }
        } catch (Throwable ignored) { }
        return false
    }

    private static void ensureOnNodeCompletedHook() {
        def existing = Object.metaClass.getMetaMethod(
                "onNodeCompleted",
                [FactoryBuilderSupport, Object, Object] as Class[]
        )
        if (existing != null) return

        Object.metaClass.'static'.onNodeCompleted = { FactoryBuilderSupport builder, Object parent, Object node ->
            if (node == null) return

            def applyOne = { obj ->
                if (obj == null) return
                try {
                    if (obj.metaClass.respondsTo(obj, "bind")) {
                        obj.bind()
                    } else if (obj.metaClass.respondsTo(obj, "apply")) {
                        obj.apply()
                    } else if (obj.metaClass.respondsTo(obj, "applyTo", FactoryBuilderSupport)) {
                        obj.applyTo(builder)
                    } else if (obj.metaClass.respondsTo(obj, "call")) {
                        obj.call()
                    }
                } catch (Throwable ignored) {
                    // legacy best-effort
                }
            }

            if (node instanceof Collection) node.each { applyOne(it) }
            else applyOne(node)
        }
    }

    // ----------------------------------------------------------------------
    // Phase 2: List/Map/Set coercions to FX observable collections
    // ----------------------------------------------------------------------

    private static void enhanceCollectionCoercions() {
        def origListAsType = List.metaClass.getMetaMethod("asType", [Class] as Class[])
        List.metaClass {
            asType << { Class clazz ->
                if (clazz == ObservableList) {
                    FXCollections.observableArrayList(delegate)
                } else if (origListAsType != null) {
                    origListAsType.invoke(delegate, clazz)
                } else {
                    InvokerHelper.asType(delegate, clazz)
                }
            }
        }

        def origMapAsType = Map.metaClass.getMetaMethod("asType", [Class] as Class[])
        Map.metaClass {
            asType << { Class clazz ->
                if (clazz == ObservableMap) {
                    FXCollections.observableMap(delegate)
                } else if (origMapAsType != null) {
                    origMapAsType.invoke(delegate, clazz)
                } else {
                    InvokerHelper.asType(delegate, clazz)
                }
            }
        }

        def origSetAsType = Set.metaClass.getMetaMethod("asType", [Class] as Class[])
        Set.metaClass {
            asType << { Class clazz ->
                if (clazz == ObservableSet) {
                    FXCollections.observableSet(delegate)
                } else if (origSetAsType != null) {
                    origSetAsType.invoke(delegate, clazz)
                } else {
                    InvokerHelper.asType(delegate, clazz)
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Phase 3: xxx() -> xxxProperty() shortcut (chained methodMissing)
    // ----------------------------------------------------------------------

    private static void enhancePropertyShortcutMethodMissing() {
        chainPropertyShortcutMethodMissing(Node, "Node")
        chainPropertyShortcutMethodMissing(Scene, "Scene")
        chainPropertyShortcutMethodMissing(Stage, "Stage")
    }

    private static void chainPropertyShortcutMethodMissing(Class targetType, String label) {
        def prevMM = targetType.metaClass.getMetaMethod("methodMissing", [String, Object] as Class[])
        targetType.metaClass.methodMissing = { String name, args ->
            def fxName = "${name}Property"
            if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                def meth = { Object[] varargs -> delegate."${name}Property"() }
                targetType.metaClass."$name" = meth
                return meth(args as Object[])
            }
            if (prevMM != null) return prevMM.invoke(delegate, name, args)
            throw new MissingMethodException(name, delegate.class, args)
        }
    }

    // ----------------------------------------------------------------------
    // Phase 4: OPTIONAL WebEngine handler sugar
    // ----------------------------------------------------------------------

    private static void enhanceWebEngineSugarIfPresent() {
        // Preserve legacy behavior: skip for eglfb platform
        if (System.properties['javafx.platform'] == 'eglfb') return

        if (!HAS_JAVAFX_WEB) {
            log.debug("JavaFX Web not present; skipping WebEngine enhancements")
            return
        }

        Class webEngineClass = loadClass("javafx.scene.web.WebEngine")
        if (webEngineClass == null) return

        // Apply the exact same metaClass additions as before, but without importing WebEngine.
        webEngineClass.metaClass {
            confirmHandler << { Closure closure -> delegate.setConfirmHandler(closure as Callback) }
            createPopupHandler << { Closure closure -> delegate.setCreatePopupHandler(closure as Callback) }
            promptHandler << { Closure closure -> delegate.setPromptHandler(closure as Callback) }

            onAlert << { Closure closure -> delegate.setOnAlert(closure as EventHandler) }
            onResized << { Closure closure -> delegate.setOnResized(closure as EventHandler) }
            onStatusChanged << { Closure closure -> delegate.setOnStatusChanged(closure as EventHandler) }
            onVisibilityChanged << { Closure closure -> delegate.setOnVisibilityChanged(closure as EventHandler) }
        }
    }

    // ----------------------------------------------------------------------
    // Capability + classloading helpers
    // ----------------------------------------------------------------------

    private static boolean hasClass(String fqn) {
        try {
            Class.forName(fqn, false, GroovyFXEnhancer.classLoader)
            return true
        } catch (Throwable ignored) {
            return false
        }
    }

    private static Class loadClass(String fqn) {
        try {
            return Class.forName(fqn, false, GroovyFXEnhancer.classLoader)
        } catch (Throwable ignored) {
            return null
        }
    }
}
