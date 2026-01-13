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

import groovy.util.logging.Slf4j
import groovyx.javafx.event.GroovyCallback
import groovyx.javafx.event.GroovyChangeListener
import groovyx.javafx.event.GroovyEventHandler
import groovyx.javafx.event.GroovyInvalidationListener
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.transform.Transform

/**
 *
 * @author jimclarke
 */
@Slf4j
public abstract class AbstractNodeFactory extends AbstractFXBeanFactory {

    public static def nodeEvents = [
            'onContextMenuRequested',
            'onDragDetected',
            'onDragDone',
            'onDragDropped',
            'onDragEntered',
            'onDragExited',
            'onDragOver',
            'onInputMethodTextChanged',
            'onKeyPressed',
            'onKeyReleased',
            'onKeyTyped',
            'onMouseClicked',
            'onMouseDragEntered',
            'onMouseDragExited',
            'onMouseDragged',
            'onMouseDragOver',
            'onMouseDragReleased',
            'onMouseEntered',
            'onMouseExited',
            'onMouseMoved',
            'onMousePressed',
            'onMouseReleased',
            'onRotate',
            'onRotationFinished',
            'onRotationStarted',
            'onScroll',
            'onScrollFinished',
            'onScrollStarted',
            'onSwipeDown',
            'onSwipeLeft',
            'onSwipeRight',
            'onSwipeUp',
            'onTouchMoved',
            'onTouchPressed',
            'onTouchReleased',
            'onTouchStationary',
            'onZoom',
            'onZoomFinished',
            'onZoomStarted'
    ]

    public AbstractNodeFactory(Class beanClass) {
        super(beanClass)
    }

    public AbstractNodeFactory(Class beanClass, boolean leaf) {
        super(beanClass, leaf)
    }

    public boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        // Store attributes in context for potential use in setChild (e.g., FormLayout)
        builder.context['_attrs'] = attributes ? new HashMap(attributes) : [:]

        for (v in nodeEvents) {
            if (attributes.containsKey(v)) {
                def val = attributes.remove(v)
                if (val instanceof Closure) {
                    FXHelper.setPropertyOrMethod(node, v, new GroovyEventHandler((Closure) val))
                } else if (val instanceof EventHandler) {
                    FXHelper.setPropertyOrMethod(node, v, val)
                }
            }
        }
        def parent = builder.context.get(FactoryBuilderSupport.CURRENT_NODE)
        handleLayoutConstraints(parent, node, attributes)
        return super.onHandleNodeAttributes(builder, node, attributes)
    }

    @Override
    boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {
        if (childContent == null) return true

        // Keep original owner/thisObject (script), but delegate to the node.
        Closure c = childContent.rehydrate(node, childContent.owner, childContent.thisObject)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            c.call()
        } catch (Throwable t) {
            log.error("Exception building children for ${node?.getClass()?.name}", t)
            throw t
        }

        return true
    }

    public void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        switch (child) {
            case GroovyEventHandler:
                FXHelper.setPropertyOrMethod(parent, child.property, child)
                break
            case GroovyChangeListener:
                FXHelper.setPropertyOrMethod(parent, child.property, child)
                break
            case GroovyInvalidationListener:
                FXHelper.setPropertyOrMethod(parent, child.property, child)
                break
            case GroovyCallback:
                if (child?.property)
                    FXHelper.setPropertyOrMethod(parent, child.property, child)
                break
            case Image:
                // for imageviews
                if (parent instanceof ImageView) {
                    parent.setImage(child)
                }
                break
            case Transform:
                // for nodes
                if (parent instanceof Node) {
                    parent.getTransforms().add(child)
                }
                break
            default:
                Object.setChild(builder, parent, child)
        }
    }

    private static def doEnum = { Class cls, value ->
        value = FXHelper.getValue(value)
        if (value == null) return null
        if (cls.isInstance(value)) return value
        if (value instanceof Boolean && cls == Priority) {
            return value ? Priority.ALWAYS : Priority.NEVER
        }
        return Enum.valueOf(cls, value.toString().trim().toUpperCase())
    }

    private static def doInsets = { value ->
        if (value == null) return null
        if (Number.class.isAssignableFrom(value.getClass())) {
            value = new Insets(value, value, value, value)
        } else if (List.class.isAssignableFrom(value.getClass())) {
            switch (value.size()) {
                case 0:
                    value = Insets.EMPTY
                    break
                case 1:
                    // top, right, bottom, left
                    value = new Insets(value[0], value[0], value[0], value[0])
                    break
                case 2:
                    value = new Insets(value[0], value[1], value[0], value[1])
                    break
                case 3:
                    value = new Insets(value[0], value[1], value[2], value[1])
                    break
                default:
                    value = new Insets(value[0], value[1], value[2], value[3])
                    break
            }
        } else if (value.toString().toUpperCase() == 'EMPTY') {
            value = Insets.EMPTY
        }
        return value
    }

    private static Integer toInt(Object v) {
        if (v == null) return null
        if (v instanceof Integer) return (Integer) v
        if (v instanceof Number) return ((Number) v).intValue()
        try {
            return Integer.valueOf(v.toString())
        } catch (ignored) {
            return null
        }
    }

    private handleLayoutConstraints(Object parent, Object node, Map attributes) {
        if (parent == null) return
        if (!(node instanceof Node)) return

        Node n = (Node) node
        boolean isGrid = parent instanceof GridPane

        def val

        // Halignment
        val = attributes.remove("halignment")
        if (val != null) {
            tryCall(parent, "setHalignment", n, doEnum.call(HPos, val))
        }

        // Valignment
        val = attributes.remove("valignment")
        if (val != null) {
            tryCall(parent, "setValignment", n, doEnum.call(VPos, val))
        }

        // column / columnIndex / col
        val = attributes.remove("column") ?: attributes.remove("columnIndex") ?: attributes.remove("col")
        if (val != null) {
            Integer iv = toInt(val)
            if (isGrid) {
                GridPane.setColumnIndex(n, iv)
            } else {
                tryCall(parent, "setColumnIndex", n, iv != null ? iv : val)
            }
        }

        // row / rowIndex
        val = attributes.remove("row") ?: attributes.remove("rowIndex")
        if (val != null) {
            Integer iv = toInt(val)
            if (isGrid) {
                GridPane.setRowIndex(n, iv)
            } else {
                tryCall(parent, "setRowIndex", n, iv != null ? iv : val)
            }
        }

        // columnSpan aliases: columnSpan, colSpan, colspan, cols
        val = attributes.remove("columnSpan") ?: attributes.remove("colSpan") ?: attributes.remove("colspan") ?: attributes.remove("cols")
        if (val != null) {
            Integer iv = toInt(val)
            if (isGrid) {
                GridPane.setColumnSpan(n, iv)
            } else {
                tryCall(parent, "setColumnSpan", n, iv != null ? iv : val)
            }
        }

        // rowSpan aliases: rowSpan, rowspan, rows
        val = attributes.remove("rowSpan") ?: attributes.remove("rowspan") ?: attributes.remove("rows")
        if (val != null) {
            Integer iv = toInt(val)
            if (isGrid) {
                GridPane.setRowSpan(n, iv)
            } else {
                tryCall(parent, "setRowSpan", n, iv != null ? iv : val)
            }
        }

        // span shorthand: span: [colSpan, rowSpan] or span: colSpan
        val = attributes.remove("span")
        if (val != null) {
            def cspan = null
            def rspan = null
            if (val instanceof List && val.size() >= 2) {
                cspan = val[0]
                rspan = val[1]
            } else {
                cspan = val
            }

            Integer csi = toInt(cspan)
            Integer rsi = toInt(rspan)

            if (cspan != null) {
                if (isGrid) GridPane.setColumnSpan(n, csi)
                else tryCall(parent, "setColumnSpan", n, csi != null ? csi : cspan)
            }
            if (rspan != null) {
                if (isGrid) GridPane.setRowSpan(n, rsi)
                else tryCall(parent, "setRowSpan", n, rsi != null ? rsi : rspan)
            }
        }

        // Hgrow
        val = attributes.remove("hgrow")
        if (val != null) {
            def p = (val instanceof Boolean) ? (val ? Priority.ALWAYS : Priority.NEVER) : doEnum.call(Priority, val)
            if (isGrid) {
                GridPane.setHgrow(n, p)
            } else {
                tryCall(parent, "setHgrow", n, p)
            }
            try {
                HBox.setHgrow(n, p)
            } catch (ignored) { }
        }

        // Vgrow
        val = attributes.remove("vgrow")
        if (val != null) {
            def p = (val instanceof Boolean) ? (val ? Priority.ALWAYS : Priority.NEVER) : doEnum.call(Priority, val)
            if (isGrid) {
                GridPane.setVgrow(n, p)
            } else {
                tryCall(parent, "setVgrow", n, p)
            }
            try {
                VBox.setVgrow(n, p)
            } catch (ignored) { }
        }

        // Margin
        val = attributes.remove("margin")
        if (val != null) {
            def ins = doInsets.call(val)
            if (isGrid) {
                GridPane.setMargin(n, ins)
            } else {
                tryCall(parent, "setMargin", n, ins)
            }
        }

        // Alignment
        val = attributes.remove("alignment")
        if (val != null) {
            tryCall(parent, "setAlignment", n, doEnum.call(Pos, val))
        }
    }

    private static void tryCall(def target, String method, Object... args) {
        try {
            target.invokeMethod(method, args)
        } catch (MissingMethodException ignored) {
            // parent doesn't support that constraint, ignore
        }
    }
}
