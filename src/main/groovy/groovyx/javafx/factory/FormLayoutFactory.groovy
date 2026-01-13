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

import groovyx.javafx.components.FormField
import groovyx.javafx.components.FormLayout
import javafx.beans.value.ChangeListener
import javafx.scene.Node
import javafx.scene.control.Label

/**
 * Factory for the modern FormLayout component.
 *
 * Preferred contract:
 *   formLayout {
 *     field(label: "...", validate: { ... }) { <one Node> }
 *   }
 *
 * Fallback:
 *   formLayout { <plain Nodes> }  // added directly to children
 */
class FormLayoutFactory extends AbstractFXBeanFactory {

    FormLayoutFactory() {
        super(FormLayout)
    }

    @Override
    boolean isLeaf() { false }

    @Override
    boolean isHandlesNodeChildren() { true }  // <-- critical for setChild routing

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        def inst = super.newInstance(builder, name, value, attributes)
        return inst
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // do NOT add fields here
        super.setChild(builder, parent, child)
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        super.onNodeCompleted(builder, parent, node)
    }
}