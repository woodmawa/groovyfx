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

import javafx.scene.Scene
import javafx.stage.Stage

/**
 * StageFactory - creates a JavaFX Stage and wires Scene/stylesheets.
 *
 * Headless-safe: does NOT call show().
 */
class StageFactory extends AbstractNodeFactory {

    private static final String PENDING_STYLESHEETS_KEY = "__pendingStageStylesheets"

    StageFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        Stage stage = (Stage) super.newInstance(builder, name, value, attributes)

        // Convenience: stage("Title") { ... }
        if (value != null && !(value instanceof Stage)) {
            stage.title = value.toString()
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
        return v?.endsWith(".css") || v?.startsWith("data:text/css") || v?.startsWith("http://") || v?.startsWith("https://")
    }
}
