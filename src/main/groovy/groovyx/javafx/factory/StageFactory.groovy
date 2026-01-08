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
import javafx.scene.Scene
import javafx.stage.Stage

/**
 * StageFactory - creates a JavaFX Stage and wires Scene/stylesheets.
 *
 * Default remains headless-safe (does NOT call show()).
 * If a legacy flag like visible:true / show:true / autoShow:true is provided,
 * it will trigger show() on nodeCompleted.
 */
class StageFactory extends AbstractNodeFactory {

    private static final String PENDING_STYLESHEETS_KEY = "__pendingStageStylesheets"

    /**
     * Map<String, List<Object>> where entry = flagName -> [value, Closure(Stage, value)]
     * Stored on Stage.properties so it survives builder scoping.
     */
    private static final String PENDING_STAGE_TRIGGERS_KEY = "__pendingStageTriggers"

    /**
     * Legacy flag aliases we accept on stage(...) for backwards compatibility.
     */
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

        // ---- Legacy flags (remove from attributes BEFORE super/newInstance applies bean props) ----
        // Collect whichever legacy flags are present; last one wins if multiple supplied.
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

        // Create stage normally
        Stage stage = (Stage) super.newInstance(builder, name, value, attributes)

        // Convenience: stage("Title") { ... }
        if (value != null && !(value instanceof Stage)) {
            stage.title = value.toString()
        }

        // Defer any trigger actions until nodeCompleted (stage is fully wired)
        if (legacyFlagName != null) {
            queueStageTrigger(stage, legacyFlagName, legacyFlagValue) { Stage s, Object v ->
                if (truthy(v)) s.show()
                else s.hide()
            }
        }

        return stage
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

    /**
     * Execute deferred stage triggers when the stage node is completed.
     * This is where legacy flags like visible:true can safely call show().
     */
    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        super.onNodeCompleted(builder, parent, node)

        if (!(node instanceof Stage)) return
        Stage stage = (Stage) node

        def triggers = (Map<String, List>) stage.properties.remove(PENDING_STAGE_TRIGGERS_KEY)
        if (!triggers) return

        // Execute in insertion order (LinkedHashMap default in Groovy literal),
        // but practically we only store one (last-wins) unless you change that.
        triggers.each { String flag, List tuple ->
            if (!tuple || tuple.size() < 2) return
            def flagValue = tuple[0]
            def action = tuple[1]
            if (action instanceof Closure) {
                ((Closure) action).call(stage, flagValue)
            }
        }
    }

    private static void queueStageTrigger(Stage stage, String flagName, Object flagValue, Closure action) {
        def props = stage.properties
        def triggers = (Map<String, List>) props.get(PENDING_STAGE_TRIGGERS_KEY)
        if (triggers == null) {
            triggers = [:]  // LinkedHashMap in Groovy
            props.put(PENDING_STAGE_TRIGGERS_KEY, triggers)
        }
        // Last-one-wins if multiple legacy flags were present
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
        // No real scene yet: queue it on stage properties
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