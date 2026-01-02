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

import groovyx.javafx.appsupport.Action
import groovyx.javafx.event.GroovyCallback
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ButtonBase
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tooltip

import static groovyx.javafx.factory.ActionFactory.applyAction
import static groovyx.javafx.factory.ActionFactory.extractActionParams

/**
 *
 * @author jimclarke
 */
class LabeledFactory extends AbstractNodeFactory {

    LabeledFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        Action action = null
        Map actionParams = [:]
        if (value instanceof Action) {
            action = value
            value = null
            actionParams = extractActionParams(attributes)
        }

        def control = super.newInstance(builder, name, value, attributes)

        if (control instanceof ButtonBase && action) {
            applyAction(control, action, actionParams)
        }

        if (value != null) {
            try {
                control.text = value.toString()
            } catch (MissingPropertyException ignored) {
                // not all Labeled-ish controls expose text the same way; ignore
            }
        }
        return control
    }

    @Override
    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (node instanceof ChoiceBox) {
            List items = attributes.remove("items")
            if (items) {
                if (!(items instanceof ObservableList)) {
                    items = FXCollections.observableArrayList(items)
                }
                node.items = items
            }
        }
        return super.onHandleNodeAttributes(builder, node, attributes)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        switch (child) {
            case Tooltip:
                parent.tooltip = child
                break

            case ContextMenu:
                parent.contextMenu = child
                break

            case Node:
                parent.graphic = child
                break

            case GroovyCallback:
                if ((parent instanceof ChoiceBox) && (child.property == "onSelect")) {
                    parent.selectionModel.selectedItemProperty().addListener(new ChangeListener() {
                        void changed(final ObservableValue observable, final Object oldValue, final Object newValue) {
                            builder.defer({ child.closure.call(parent, newValue) })
                        }
                    })
                }
                break

            default:
                super.setChild(builder, parent, child)
        }
    }
}
