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
import javafx.scene.Node

/**
 * graphic { <node> } wrapper:
 * - captures one Node child
 * - assigns it to parent.graphic when completed
 */
class GraphicFactory extends AbstractFXBeanFactory {

    static class GraphicWrapper {
        Node node
    }

    GraphicFactory() {
        super(GraphicWrapper)
    }

    /** Backward-compat: SceneGraphBuilder.registerControls calls new GraphicFactory(Class). */
    GraphicFactory(Class ignored) {
        this()
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        return new GraphicWrapper()
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Node) {
            parent.node = (Node) child
            return
        }
        super.setChild(builder, parent, child)
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        if (node instanceof GraphicWrapper && node.node != null) {
            if (parent?.metaClass?.hasProperty(parent, "graphic")) {
                parent.graphic = node.node
                return
            }
        }
        super.onNodeCompleted(builder, parent, node)
    }
}
