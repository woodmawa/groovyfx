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
