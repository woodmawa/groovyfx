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
 *
 * Hardenings:
 *  - Chains existing methodMissing handlers instead of clobbering them
 *  - Adds null-safe fallbacks for List/Map asType interception
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
        def existing = Object.metaClass.getMetaMethod(
                "onHandleNodeAttributes",
                [FactoryBuilderSupport, Object, Map] as Class[]
        )
        if (existing == null) {
            Object.metaClass.'static'.onHandleNodeAttributes = { FactoryBuilderSupport builder, Object node, Map attrs ->
                // Must be safe when attrs is empty (common in tests)
                if (attrs == null || attrs.isEmpty()) return attrs
                // Conservative: do not consume; let factories handle it.
                return attrs
            }
        }

        // ------------------------------------------------------------------
        // 1b) REQUIRED by groovyx.javafx.factory.AbstractNodeFactory
        // ------------------------------------------------------------------
        def existingSetChild = Object.metaClass.getMetaMethod(
                "setChild",
                [FactoryBuilderSupport, Object, Object] as Class[]
        )
        if (existingSetChild == null) {
            Object.metaClass.'static'.setChild = { FactoryBuilderSupport builder, Object parent, Object child ->
                if (parent == null || child == null) return

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

                if (effectiveChild instanceof javafx.scene.Node) {
                    // 1) graphic property
                    try {
                        def p = parent.metaClass.hasProperty(parent, "graphic")
                        if (p != null) {
                            parent.graphic = effectiveChild
                            return
                        }
                    } catch (Throwable ignored) { }

                    // 2) setGraphic(Node)
                    try {
                        if (parent.metaClass.respondsTo(parent, "setGraphic", javafx.scene.Node)) {
                            parent.setGraphic(effectiveChild)
                            return
                        }
                    } catch (Throwable ignored) { }

                    // 3) Parent.children
                    try {
                        def chProp = parent.metaClass.hasProperty(parent, "children")
                        if (chProp != null && parent.children instanceof Collection) {
                            parent.children.add(effectiveChild)
                            return
                        }
                    } catch (Throwable ignored) { }

                    // 4) SplitPane.items (Nodes only)
                    try {
                        if (parent instanceof SplitPane) {
                            parent.items.add(effectiveChild)
                            return
                        }
                    } catch (Throwable ignored) { }
                }

                return
            }
        }

        // ------------------------------------------------------------------
        // 1c) REQUIRED by bindings + some factories
        // ------------------------------------------------------------------
        def existingOnNodeCompleted = Object.metaClass.getMetaMethod(
                "onNodeCompleted",
                [FactoryBuilderSupport, Object, Object] as Class[]
        )
        if (existingOnNodeCompleted == null) {
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

        // ------------------------------------------------------------------
        // 2) Collection coercions
        // ------------------------------------------------------------------
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

        // ------------------------------------------------------------------
        // 3) Shortcut: xxx() -> xxxProperty() for Node/Scene/Stage (CHAINED)
        // ------------------------------------------------------------------
        def prevNodeMM = Node.metaClass.getMetaMethod("methodMissing", [String, Object] as Class[])
        Node.metaClass.methodMissing = { String name, args ->
            def fxName = "${name}Property"
            if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                def meth = { Object[] varargs -> delegate."${name}Property"() }
                Node.metaClass."$name" = meth
                return meth(args as Object[])
            }
            if (prevNodeMM != null) return prevNodeMM.invoke(delegate, name, args)
            throw new MissingMethodException(name, delegate.class, args)
        }

        def prevSceneMM = Scene.metaClass.getMetaMethod("methodMissing", [String, Object] as Class[])
        Scene.metaClass.methodMissing = { String name, args ->
            def fxName = "${name}Property"
            if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                def meth = { Object[] varargs -> delegate."${name}Property"() }
                Scene.metaClass."$name" = meth
                return meth(args as Object[])
            }
            if (prevSceneMM != null) return prevSceneMM.invoke(delegate, name, args)
            throw new MissingMethodException(name, delegate.class, args)
        }

        def prevStageMM = Stage.metaClass.getMetaMethod("methodMissing", [String, Object] as Class[])
        Stage.metaClass.methodMissing = { String name, args ->
            def fxName = "${name}Property"
            if (delegate.metaClass.respondsTo(delegate, fxName, InvokerHelper.EMPTY_ARGUMENTS)) {
                def meth = { Object[] varargs -> delegate."${name}Property"() }
                Stage.metaClass."$name" = meth
                return meth(args as Object[])
            }
            if (prevStageMM != null) return prevStageMM.invoke(delegate, name, args)
            throw new MissingMethodException(name, delegate.class, args)
        }

        // ------------------------------------------------------------------
        // 4) WebEngine handler sugar
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
