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

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane

/**
 * ContainerFactory - adds child Nodes to containers.
 *
 * Historically this used Parent.getChildren(), but that method is protected in JavaFX.
 * Relying on reflective access is fragile under modern Java/module rules.
 *
 * We now explicitly support the public "children" APIs:
 * - Pane.getChildren()
 * - Group.getChildren()
 *
 * Other Parent subclasses do not have a public mutable children list.
 */
class ContainerFactory extends AbstractNodeFactory {
    private static final String BUILDER_LIST_PROPERTY = "__builderList"

    ContainerFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // BorderPane is special: it can accept either:
        //  - a Node (legacy default -> center)
        //  - a BorderPanePosition wrapper (top/left/right/bottom/center routing)
        if (parent instanceof BorderPane) {
            if (child instanceof BorderPanePosition) {
                ((BorderPanePosition) child).applyTo((BorderPane) parent)
                return
            }
            if (child instanceof Node) {
                // legacy behavior: default BorderPane nesting maps to center
                ((BorderPane) parent).setCenter((Node) child)
                return
            }
            // fall through for other child types
        }

        if (child instanceof Node) {
            if (addToChildren(parent, (Node) child)) return

        } else if (child instanceof List) {
            if (addAllToChildren(parent, (List) child)) return

        } else if (parent instanceof GridPane && child instanceof RowColumnInfo) {
            GridPane grid = (GridPane) parent
            RowColumnInfo rci = (RowColumnInfo) child

            if (rci.rowInfo != null) {
                if (rci.range != null) {
                    for (i in rci.range) {
                        grid.getRowInfo().add(i, rci.rowInfo)
                    }
                } else {
                    grid.getRowInfo().add(rci.rowInfo)
                }
                return
            }

            if (rci.columnInfo != null) {
                if (rci.range != null) {
                    for (i in rci.range) {
                        grid.getColumnInfo().add(i, rci.columnInfo)
                    }
                } else {
                    grid.getColumnInfo().add(rci.columnInfo)
                }
                return
            }
        }

        super.setChild(builder, parent, child)
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        def builderList = builder.context.remove(BUILDER_LIST_PROPERTY)
        if (builderList && node instanceof Parent) {
            builderList.each {
                def built = it.build()
                if (built instanceof Node) {
                    addToChildren(node, (Node) built)
                }
            }
        }
    }

    private static boolean addToChildren(Object parent, Node child) {
        if (parent instanceof Pane) {
            parent.children.add(child)
            return true
        }
        if (parent instanceof Group) {
            parent.children.add(child)
            return true
        }
        return false
    }

    private static boolean addAllToChildren(Object parent, List children) {
        if (parent instanceof Pane) {
            parent.children.addAll(children)
            return true
        }
        if (parent instanceof Group) {
            parent.children.addAll(children)
            return true
        }
        return false
    }
}
