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

import groovy.util.FactoryBuilderSupport
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.stage.WindowEvent

/**
 * StageFactory - creates a JavaFX Stage and wires Scene/stylesheets.
 *
 * * Adds GroovyFX DSL niceties:
 *  - id: 'name'  -> registers the stage in the builder (NOT a Stage property)
 *  - onShown/onHidden/onCloseRequest/onShowing/onHiding -> Stage event handlers
 *
 * Default remains headless-safe (does NOT call show()).
 * If a legacy flag like visible:true / show:true / autoShow:true is provided,
 * it will trigger show() on nodeCompleted.
 */
class StageFactory extends AbstractNodeFactory {

    private static final String PENDING_STYLESHEETS_KEY   = "__pendingStageStylesheets"
    private static final String PENDING_STAGE_TRIGGERS_KEY = "__pendingStageTriggers"

    private static final List<String> LEGACY_SHOW_FLAGS = [
            'visible',
            'show',
            'autoShow',
            'display',
            'autoShowOnBuild'
    ]

    StageFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        // ---- Legacy flags (remove BEFORE bean property application) ----
        def legacyFlagName = null
        def legacyFlagValue = null
        if (attributes) {
            LEGACY_SHOW_FLAGS.each { String k ->
                if (attributes.containsKey(k)) {
                    legacyFlagName = k
                    legacyFlagValue = attributes.remove(k)
                }
            }
        }

        // Create stage normally (this will apply remaining properties)
        Stage stage = (Stage) super.newInstance(builder, name, value, attributes)

        // Convenience: stage("Title") { ... }
        if (value != null && !(value instanceof Stage)) {
            stage.title = value.toString()
        }

        // Defer show/hide until nodeCompleted so scene/styles are wired
        if (legacyFlagName != null) {
            queueStageTrigger(stage, legacyFlagName, legacyFlagValue) { Stage s, Object v ->
                if (truthy(v)) s.show()
                else s.hide()
            }
        }

        return stage
    }

    /**
     * IMPORTANT: AbstractFactory expects boolean return.
     * Return true = we've handled/consumed attributes (or delegated safely).
     */
    @Override
    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (!(node instanceof Stage) || attributes == null || attributes.isEmpty()) {
            return super.onHandleNodeAttributes(builder, node, attributes)
        }

        Stage stage = (Stage) node

        // ---- GroovyFX-style id: (builder variable) ----
        def id = attributes.remove('id')
        if (id != null) {
            // register as a variable for later lookup in the script
            builder.setVariable(id.toString(), stage)
            // optional: also stash in builder context if you want:
            // builder.context[id.toString()] = stage
        }

        // ---- Stage lifecycle events (attributes, not children) ----
        // Accept either Closure or EventHandler<WindowEvent>
        attachWindowHandler(stage, 'onShowing',      attributes.remove('onShowing'))
        attachWindowHandler(stage, 'onShown',        attributes.remove('onShown'))
        attachWindowHandler(stage, 'onHiding',       attributes.remove('onHiding'))
        attachWindowHandler(stage, 'onHidden',       attributes.remove('onHidden'))
        attachWindowHandler(stage, 'onCloseRequest', attributes.remove('onCloseRequest'))

        // Now let the superclass apply remaining attributes as bean properties
        return super.onHandleNodeAttributes(builder, node, attributes)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        switch (child) {
            case Scene:
                parent.scene = child
                applyPendingStylesheets(parent)
                break

            case StylesheetRef:
                addStageStylesheet(parent, child.url)
                break

            case String:
                if (looksLikeStylesheet(child)) {
                    addStageStylesheet(parent, child)
                } else {
                    parent.title = child
                }
                break

            default:
                super.setChild(builder, parent, child)
        }
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        super.onNodeCompleted(builder, parent, node)

        if (!(node instanceof Stage)) return
        Stage stage = (Stage) node

        def triggers = (Map<String, List>) stage.properties.remove(PENDING_STAGE_TRIGGERS_KEY)
        if (!triggers) return

        triggers.each { String flag, List tuple ->
            if (!tuple || tuple.size() < 2) return
            def flagValue = tuple[0]
            def action = tuple[1]
            if (action instanceof Closure) {
                ((Closure) action).call(stage, flagValue)
            }
        }
    }

    // ---------------- helpers ----------------

    private static void attachWindowHandler(Stage stage, String propName, Object handler) {
        if (handler == null) return

        EventHandler<WindowEvent> eh
        if (handler instanceof EventHandler) {
            eh = (EventHandler<WindowEvent>) handler
        } else if (handler instanceof Closure) {
            Closure c = (Closure) handler
            eh = { WindowEvent e ->
                // closure can be { -> ... } or { evt -> ... }
                if (c.maximumNumberOfParameters == 0) c.call()
                else c.call(e)
            } as EventHandler<WindowEvent>
        } else {
            throw new IllegalArgumentException("${propName} must be a Closure or EventHandler (got ${handler.getClass().name})")
        }

        // assign property dynamically (stage.onShown = eh, etc.)
        stage."${propName}" = eh
    }

    private static void queueStageTrigger(Stage stage, String flagName, Object flagValue, Closure action) {
        def props = stage.properties
        def triggers = (Map<String, List>) props.get(PENDING_STAGE_TRIGGERS_KEY)
        if (triggers == null) {
            triggers = [:]   // LinkedHashMap
            props.put(PENDING_STAGE_TRIGGERS_KEY, triggers)
        }
        triggers[flagName] = [flagValue, action]
    }

    private static boolean truthy(Object v) {
        if (v == null) return false
        if (v instanceof Boolean) return (Boolean) v
        if (v instanceof Number) return ((Number) v).doubleValue() != 0d
        def s = v.toString().trim().toLowerCase()
        return (s == 'true' || s == 'yes' || s == 'y' || s == '1' || s == 'on')
    }

    private static void addStageStylesheet(Stage stage, String url) {
        if (stage.scene != null) {
            stage.scene.stylesheets.add(url)
            return
        }
        def props = stage.properties
        def pending = (List<String>) props.get(PENDING_STYLESHEETS_KEY)
        if (pending == null) {
            pending = []
            props.put(PENDING_STYLESHEETS_KEY, pending)
        }
        pending.add(url)
    }

    private static void applyPendingStylesheets(Stage stage) {
        if (stage.scene == null) return
        def pending = (List<String>) stage.properties.remove(PENDING_STYLESHEETS_KEY)
        if (pending) {
            stage.scene.stylesheets.addAll(pending)
        }
    }

    private static boolean looksLikeStylesheet(String s) {
        def v = s?.toLowerCase()
        return v?.endsWith(".css") ||
                v?.startsWith("data:text/css") ||
                v?.startsWith("http://") ||
                v?.startsWith("https://")
    }
}