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


import groovyx.javafx.event.GroovyEventHandler
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.TreeItem

/**
 * @author jimclarke
 * minor adaptions by hackergarten
 */
class TreeItemFactory extends AbstractFXBeanFactory {

    public static def treeItemEvents
    static {
        treeItemEvents = [
                "onBranchCollapse": TreeItem.branchCollapsedEvent(),
                "onBranchExpand" : TreeItem.branchExpandedEvent(),
                "onChildrenModification" : TreeItem.childrenModificationEvent(),
                "onGraphicChanged" : TreeItem.graphicChangedEvent(),
                "onTreeNotification" : TreeItem.treeNotificationEvent(),
                "onValueChanged" : TreeItem.valueChangedEvent()
        ]
        treeItemEvents.onExpandedItemCountChange = TreeItem.expandedItemCountChangeEvent()
    }

    public TreeItemFactory() {
        super(TreeItem)
    }

    public TreeItemFactory(beanClass) {
        super(beanClass)
    }

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        TreeItem item = super.newInstance(builder, name, value, attributes)
        if(!checkValue(name, value)) {
            item.value = value
        }
        item;
    }

    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // Some "property/event" nodes apply themselves and return null.
        if (child == null) return

        if (child instanceof TreeItem) {
            parent.children.add(child)
            return
        }

        if (child instanceof Node) {
            // Treat embedded Node as graphic unless a dedicated graphic{} node handled it
            parent.graphic = child
            return
        }

        // GroovyFX event wrapper (old path)
        if (child instanceof GroovyEventHandler) {
            def name = child.name
            if (treeItemEvents.containsKey(name)) {
                setEventHandler(parent as TreeItem, name, child as EventHandler)
                return
            }
            // If it's some other event spec, let it pass quietly (legacy tolerance)
            return
        }

        // ClosureHandlerFactory "spec" (new path you're hitting):
        // groovyx.javafx.factory.ClosureHandlerFactory$HandlerSpec
        if (child?.class?.name == 'groovyx.javafx.factory.ClosureHandlerFactory$HandlerSpec') {
            def name = child.name
            def handler = (child.hasProperty('handler') ? child.handler : null)
            if (handler == null && child.hasProperty('closure')) handler = child.closure

            if (handler != null && treeItemEvents.containsKey(name)) {
                setEventHandler(parent as TreeItem, name, handler as EventHandler)
            }
            return
        }

        // graphic { ... } in GroovyFX often returns a wrapper, not a Node
        if (child?.class?.name == 'groovyx.javafx.factory.GraphicFactory$GraphicWrapper') {
            def wrapped =
                    (child.hasProperty('node') ? child.node : null) ?:
                            (child.hasProperty('graphic') ? child.graphic : null) ?:
                                    (child.hasProperty('value') ? child.value : null)

            if (wrapped instanceof Node) {
                parent.graphic = wrapped
                return
            }

            // If wrapper didn't contain a Node, ignore (or throw if you prefer strictness)
            return
        }

        // Last-resort tolerance: silently ignore known DSL wrapper types
        if (child?.class?.name?.startsWith('groovyx.javafx.factory.')) {
            return
        }

        // Newer paths may return a raw handler; ignore here.
        if (child instanceof EventHandler) return

        throw new Exception(
                "In a TreeItem, value must be an instanceof TreeItem, Node, or an event to be used as embedded content. " +
                        "Got: ${child.getClass().name}"
        )
    }

    public boolean onHandleNodeAttributes( FactoryBuilderSupport builder, Object node,
            Map attributes ) {
        for(v in treeItemEvents) {
            if(attributes.containsKey(v)) {
                def val = attributes.remove(v);
                if(val instanceof Closure) {
                    setEventHandler(node, v, val as EventHandler);
                }else if(val instanceof EventHandler) {
                    setEventHandler(node, v, val);
                }
            }
        }
        return super.onHandleNodeAttributes(builder, node, attributes);
    }

    void setEventHandler(TreeItem item, String property, EventHandler handler) {
        def eventType = treeItemEvents[property]
        item.addEventHandler(eventType, handler)
    }

}
