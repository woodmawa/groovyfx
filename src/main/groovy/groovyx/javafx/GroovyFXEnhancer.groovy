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
import javafx.scene.web.WebEngine

/**
 * Restores legacy GroovyFX metaclass enhancements expected by factories.
 *
 * Key fix:
 *  - Adds Object.onHandleNodeAttributes(builder, node, attrs) static hook
 *    used by AbstractNodeFactory.
 *
 * Also fixes:
 *  - Stage.metaClass methodMissing incorrectly writing to Scene.metaClass.
 */
@Slf4j
class GroovyFXEnhancer {

    private static volatile boolean enhanced = false
    private static final Object LOCK = new Object()

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
        // 1) REQUIRED by groovyx.javafx.factory.AbstractNodeFactory
        // ------------------------------------------------------------------
        // Some factories call: Object.onHandleNodeAttributes(builder, node, attrs)
        // If this isn't installed you get your current MissingMethodException.
        def existing = Object.metaClass.getMetaMethod(
                "onHandleNodeAttributes",
                [FactoryBuilderSupport, Object, Map] as Class[]
        )
        if (existing == null) {
            Object.metaClass.'static'.onHandleNodeAttributes = { FactoryBuilderSupport builder, Object node, Map attrs ->
                // Important: must be safe when attrs is empty (common in tests)
                if (attrs == null || attrs.isEmpty()) return attrs

                // Keep behavior conservative: let FactoryBuilderSupport do the real work.
                // We do NOT consume attrs here; just return it untouched.
                // If you later want legacy consumption of some attributes, do it here.
                return attrs
            }
        }

        // ------------------------------------------------------------------
        // 1b) REQUIRED by groovyx.javafx.factory.AbstractNodeFactory
        // ------------------------------------------------------------------
        // Many factories delegate to: Object.setChild(builder, parent, child)
        // (MenuItem graphic wrapper, TitledPane graphic wrapper, etc.)
        def existingSetChild = Object.metaClass.getMetaMethod(
                "setChild",
                [FactoryBuilderSupport, Object, Object] as Class[]
        )
        if (existingSetChild == null) {
            Object.metaClass.'static'.setChild = { FactoryBuilderSupport builder, Object parent, Object child ->
                if (parent == null || child == null) return

                // Unwrap wrapper children (e.g. GraphicFactory.GraphicWrapper) by calling build()
                def effectiveChild = child
                try {
                    if (!(child instanceof javafx.scene.Node) &&
                            child.metaClass.respondsTo(child, "build", InvokerHelper.EMPTY_ARGUMENTS)) {
                        def built = child.build()
                        if (built != null) effectiveChild = built
                    }
                } catch (ignored) {
                    // keep original child
                }

                // If we ended up with a Node, try common attachment patterns
                if (effectiveChild instanceof javafx.scene.Node) {

                    // 1) graphic property (MenuItem, Labeled, TitledPane, etc.)
                    try {
                        def p = parent.metaClass.hasProperty(parent, "graphic")
                        if (p != null) {
                            parent.graphic = effectiveChild
                            return
                        }
                    } catch (ignored) { }

                    // 2) setGraphic(Node)
                    try {
                        if (parent.metaClass.respondsTo(parent, "setGraphic", javafx.scene.Node)) {
                            parent.setGraphic(effectiveChild)
                            return
                        }
                    } catch (ignored) { }

                    // 3) Parent.children (JavaFX SceneGraph)
                    try {
                        def chProp = parent.metaClass.hasProperty(parent, "children")
                        if (chProp != null && parent.children instanceof java.util.Collection) {
                            parent.children.add(effectiveChild)
                            return
                        }
                    } catch (ignored) { }

                    // 4) SplitPane.items (Nodes)
                    // Avoid generic "items" because many controls have items that are NOT Nodes.
                    try {
                        if (parent instanceof javafx.scene.control.SplitPane) {
                            parent.items.add(effectiveChild)
                            return
                        }
                    } catch (ignored) { }
                }

                // Otherwise do nothing (legacy "best effort")
                return
            }
        }

        // ------------------------------------------------------------------
        // 1c) REQUIRED by bindings + some factories
        // ------------------------------------------------------------------
        // Some parts call: Object.onNodeCompleted(builder, parent, node)
        def existingOnNodeCompleted = Object.metaClass.getMetaMethod(
                "onNodeCompleted",
                [FactoryBuilderSupport, Object, Object] as Class[]
        )
        if (existingOnNodeCompleted == null) {
            Object.metaClass.'static'.onNodeCompleted = { FactoryBuilderSupport builder, Object parent, Object node ->
                if (node == null) return

                // Apply common "binding holder" patterns if present.
                // (We keep this reflective so it doesn't hard-depend on classes.)
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
                    } catch (ignored) {
                        // best-effort (legacy behaviour)
                    }
                }

                if (node instanceof Collection) {
                    node.each { applyOne(it) }
                } else {
                    applyOne(node)
                }
            }
        }

        // ------------------------------------------------------------------
        // 2) Collection coercions (your existing behavior)
        // ------------------------------------------------------------------
        def origListAsType = List.metaClass.getMetaMethod("asType", [Class] as Class[])
        List.metaClass {
            asType << { Class clazz ->
                if (clazz == ObservableList) {
                    FXCollections.observableArrayList(delegate)
                } else {
                    origListAsType.invoke(delegate, clazz)
                }
            }
        }

        def origMapAsType = Map.metaClass.getMetaMethod("asType", [Class] as Class[])
        Map.metaClass {
            asType << { Class clazz ->
                if (clazz == ObservableMap) {
                    FXCollections.observableMap(delegate)
                } else {
                    origMapAsType.invoke(delegate, clazz)
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
                }
            }
        }

        // ------------------------------------------------------------------
        // 3) Shortcut: xxx() -> xxxProperty() for Node/Scene/Stage
        // ------------------------------------------------------------------
        Node.metaClass {
            methodMissing = { String name, args ->
                def fxName = "${name}Property"
                if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                    def meth = { Object[] varargs -> delegate."${name}Property"() }
                    Node.metaClass."$name" = meth
                    return meth(args as Object[])
                }
                throw new MissingMethodException(name, delegate.class, args)
            }
        }

        Scene.metaClass {
            methodMissing = { String name, args ->
                def fxName = "${name}Property"
                if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                    def meth = { Object[] varargs -> delegate."${name}Property"() }
                    Scene.metaClass."$name" = meth
                    return meth(args as Object[])
                }
                throw new MissingMethodException(name, delegate.class, args)
            }
        }

        Stage.metaClass {
            methodMissing = { String name, args ->
                def fxName = "${name}Property"
                if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                    def meth = { Object[] varargs -> delegate."${name}Property"() }
                    // FIX: must install on Stage.metaClass (your file installed on Scene.metaClass)
                    Stage.metaClass."$name" = meth
                    return meth(args as Object[])
                }
                throw new MissingMethodException(name, delegate.class, args)
            }
        }

        // ------------------------------------------------------------------
        // 4) WebEngine handler sugar (your existing behavior)
        // ------------------------------------------------------------------
        if (System.properties['javafx.platform'] != 'eglfb') {
            WebEngine.metaClass {
                confirmHandler << { Closure closure -> delegate.setConfirmHandler(closure as Callback) }
                createPopupHandler << { Closure closure -> delegate.setCreatePopupHandler(closure as Callback) }
                promptHandler << { Closure closure -> delegate.setPromptHandler(closure as Callback) }

                onAlert << { Closure closure -> delegate.setOnAlert(closure as EventHandler) }
                onResized << { Closure closure -> delegate.setOnResized(closure as EventHandler) }
                onStatusChanged << { Closure closure -> delegate.setOnStatusChanged(closure as EventHandler) }
                onVisibilityChanged << { Closure closure -> delegate.setOnVisibilityChanged(closure as EventHandler) }
            }
        }
    }
}
