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

import groovyx.javafx.components.FormLayout
import javafx.scene.Node
import javafx.scene.control.Label

/**
 * Factory for the modern FormLayout component.
 */
class FormLayoutFactory extends AbstractNodeFactory {
    FormLayoutFactory() {
        super(FormLayout)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof FormLayout && child instanceof Node) {
            Map attributes = builder.getContext().get(child) ?: [:]
            String labelText = attributes.remove("label")
            Closure validateClosure = (Closure) attributes.remove("validate")

            if (labelText) {
                if (validateClosure) {
                    Label errorLabel = new Label()
                    parent.addField(labelText, (Node) child, errorLabel)

                    // Simple validation logic wired to text property if it exists
                    if (child.metaClass.hasProperty(child, "text")) {
                        child.textProperty().addListener { obs, old, val ->
                            String error = validateClosure.call(val)
                            errorLabel.setText(error ?: "")
                        }
                    }
                } else {
                    parent.addField(labelText, (Node) child)
                }
            } else {
                parent.getChildren().add((Node) child)
            }
        } else {
            super.setChild(builder, parent, child)
        }
    }
}
