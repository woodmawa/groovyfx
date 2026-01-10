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

import groovyx.javafx.canvas.CanvasOperation
import groovyx.javafx.canvas.DrawOperations
import javafx.collections.FXCollections
import javafx.scene.canvas.Canvas

/**
 * CanvasFactory collects CanvasOperation children (e.g. operation { gc -> ... })
 * and builds a DrawOperations instance when the canvas node completes.
 *
 * IMPORTANT:
 *  - When setChild() is called, builder.context is the CHILD context.
 *  - The canvas' context is builder.parentContext at that point.
 *  - onNodeCompleted() runs with builder.context == the canvas context.
 *
 * So we must accumulate operations into builder.parentContext, not builder.context,
 * otherwise the list is lost and the canvas draws nothing (blank output).
 */
class CanvasFactory extends AbstractNodeFactory {

    private static final String CANVAS_OPERATIONS_LIST_PROPERTY = "__canvasOperationsList"

    CanvasFactory() {
        super(Canvas)
    }

    CanvasFactory(Class<Canvas> beanClass) {
        super(beanClass)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof CanvasOperation) {
            // Store on the CANVAS context (parent context), not the child's context.
            def ctx = builder.parentContext
            def operations = ctx.get(CANVAS_OPERATIONS_LIST_PROPERTY)

            if (!(operations instanceof List)) {
                operations = []
                ctx.put(CANVAS_OPERATIONS_LIST_PROPERTY, operations)
            }

            operations << child
        } else {
            super.setChild(builder, parent, child)
        }
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        // builder.context here is the CANVAS context, so remove from it.
        def opsList = builder.context.remove(CANVAS_OPERATIONS_LIST_PROPERTY)

        // remove(.) may return null => DO NOT call observableArrayList(null)
        def operations = (opsList instanceof Collection)
                ? FXCollections.observableArrayList(opsList as Collection)
                : FXCollections.observableArrayList()

        def dop = new DrawOperations(operations: operations, canvas: node)
        dop.draw()
        node.userData = dop

        super.onNodeCompleted(builder, parent, node)
    }
}
