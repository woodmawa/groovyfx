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
package groovyx.javafx.binding;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MetaClass;
import groovy.lang.Reference;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A read-only JavaFX property whose value is computed by executing a Groovy {@link Closure}.
 *
 * <p>This class supports "expression bindings" by first <em>snooping</em> the closure body to
 * discover accessed property paths, then attaching listeners to those properties. When any
 * dependency changes or is invalidated, this property invalidates and/or fires change events
 * to its registered listeners.</p>
 *
 * <p>The snooping mechanism intentionally executes a cloned closure with a special delegate
 * that records property access and prevents method-call return values from being used as
 * bindable property targets.</p>
 *
 * @author jimclarke
 */
public class GroovyClosureProperty implements ReadOnlyProperty<Object>,
        ChangeListener<Object>,
        InvalidationListener {

    /**
     * Special object returned from method invocations while snooping. Any attempt to bind to
     * a property of such a return value results in a {@link DeadEndException}.
     */
    private final DeadEndObject deadEnd = new DeadEndObject();

    private Closure<?> closure;
    private Object bean;
    private String name;

    private Object oldValue = null;
    private Object newValue = null;
    private boolean valueDirty = true;

    private final List<ChangeListener<? super Object>> changeListeners = new ArrayList<>();
    private final List<InvalidationListener> invalidationListeners = new ArrayList<>();

    /**
     * Creates an unconfigured closure property. You can later call {@link #setBean(Object)},
     * {@link #setName(String)} and {@link #setClosure(Closure)}.
     */
    public GroovyClosureProperty() {
        // default constructor
    }

    /**
     * Creates a closure property with an associated bean and name.
     *
     * @param bean the bean associated with this property (may be {@code null})
     * @param name the property name (may be {@code null})
     */
    public GroovyClosureProperty(Object bean, String name) {
        this.bean = bean;
        this.name = name;
    }

    /**
     * Creates a closure property and immediately binds it by snooping the closure expression.
     *
     * @param closure the closure whose return value becomes this property's value
     */
    public GroovyClosureProperty(Closure<?> closure) {
        setClosure(closure);
    }

    /**
     * Creates a closure property with bean/name metadata and binds it to the supplied closure.
     *
     * @param bean    the bean associated with this property (may be {@code null})
     * @param name    the property name (may be {@code null})
     * @param closure the closure whose return value becomes this property's value
     */
    public GroovyClosureProperty(Object bean, String name, Closure<?> closure) {
        this.bean = bean;
        this.name = name;
        setClosure(closure);
    }

    /**
     * Sets the closure and (re)creates dependency bindings by snooping its expression.
     *
     * @param closure the closure to use (may be {@code null})
     */
    public final void setClosure(Closure<?> closure) {
        this.closure = closure;
        if (closure != null) {
            createBindings(closure);
        }
    }

    /**
     * Returns the computed value by calling the closure.
     *
     * <p>If the closure returns a {@link ReadOnlyProperty}, this method returns the nested
     * property's value instead.</p>
     *
     * @return the computed value, or {@code null} if no closure is set
     */
    @Override
    public final Object getValue() {
        Object result = null;
        if (closure != null) {
            result = closure.call();
            if (result instanceof ReadOnlyProperty<?> rop) {
                result = rop.getValue();
            }
        }
        return result;
    }

    /**
     * Returns the bean associated with this property.
     *
     * @return the bean, possibly {@code null}
     */
    @Override
    public final Object getBean() {
        return bean;
    }

    /**
     * Returns the name associated with this property.
     *
     * @return the name, possibly {@code null}
     */
    @Override
    public final String getName() {
        return name;
    }

    /**
     * Returns the current closure.
     *
     * @return the closure, possibly {@code null}
     */
    public final Closure<?> getClosure() {
        return closure;
    }

    /**
     * Sets the associated bean.
     *
     * @param bean the bean to associate (may be {@code null})
     */
    public final void setBean(Object bean) {
        this.bean = bean;
    }

    /**
     * Sets the associated name.
     *
     * @param name the name to associate (may be {@code null})
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Adds a change listener to this property.
     *
     * @param listener the listener to add
     */
    @Override
    public void addListener(ChangeListener<? super Object> listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes a change listener from this property.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(ChangeListener<? super Object> listener) {
        changeListeners.remove(listener);
    }

    /**
     * Adds an invalidation listener to this property.
     *
     * @param listener the listener to add
     */
    @Override
    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    /**
     * Removes an invalidation listener from this property.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    /**
     * Builds dependency bindings for the given closure by snooping which properties it touches.
     *
     * @param closure the closure to analyze
     */
    private void createBindings(Closure<?> closure) {
        final Snooper delegate = new Snooper();

        try {
            final Class<?> closureClass = closure.getClass();

            // Clone closure instance with a special delegate and per-field Snooper references.
            final Closure<?> closureLocalCopy;
            try {
                @SuppressWarnings("rawtypes")
                Constructor constructor = closureClass.getConstructors()[0];
                int paramCount = constructor.getParameterTypes().length;
                Object[] args = new Object[paramCount];
                args[0] = delegate;
                for (int i = 1; i < paramCount; i++) {
                    args[i] = new Reference<>(new Snooper());
                }

                boolean acc = constructor.canAccess(null);
                constructor.setAccessible(true);
                closureLocalCopy = (Closure<?>) constructor.newInstance(args);
                constructor.setAccessible(acc);

                closureLocalCopy.setResolveStrategy(Closure.DELEGATE_ONLY);

                for (Field f : closureClass.getDeclaredFields()) {
                    boolean facc = f.canAccess(null);
                    f.setAccessible(true);
                    if (f.getType() == Reference.class) {
                        delegate.getFields().put(
                                f.getName(),
                                (Snooper) ((Reference<?>) f.get(closureLocalCopy)).get()
                        );
                    }
                    f.setAccessible(facc);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error snooping closure", e);
            }

            try {
                // Execute the snooped closure. Fail fast for "dead end" binding attempts.
                closureLocalCopy.call();
            } catch (DeadEndException e) {
                throw e;
            } catch (Exception ignored) {
                // Intentionally ignore: can fail when snooping hits APIs expecting real types.
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new RuntimeException(
                    "A closure expression binding could not be created because of " + e.getClass().getName() +
                            ":\n\t" + e.getMessage()
            );
        }

        List<BindPath> rootPaths = new ArrayList<>();
        for (Map.Entry<String, Snooper> entry : delegate.getFields().entrySet()) {
            BindPath bp = new BindPath(entry.getKey(), entry.getValue());
            bp.setCurrentObject(closure);
            bp.bind();
            rootPaths.add(bp);
        }

        update();
    }

    /**
     * Recomputes {@link #newValue} and retains {@link #oldValue}.
     */
    private void update() {
        this.oldValue = this.newValue;
        this.newValue = closure != null ? closure.call() : null;
        valueDirty = false;
    }

    /**
     * Marks value dirty, recomputes, and notifies change listeners.
     */
    private void fireChanged() {
        valueDirty = true;
        update();
        if (!changeListeners.isEmpty()) {
            ChangeListener<? super Object>[] listeners = changeListeners.toArray(new ChangeListener[0]);
            for (ChangeListener<? super Object> l : listeners) {
                l.changed(this, oldValue, newValue);
            }
        }
    }

    /**
     * Marks value dirty and notifies invalidation listeners.
     */
    private void fireInvalidated() {
        valueDirty = true;
        if (!invalidationListeners.isEmpty()) {
            InvalidationListener[] listeners = invalidationListeners.toArray(new InvalidationListener[0]);
            for (InvalidationListener l : listeners) {
                l.invalidated(this);
            }
        }
    }

    /**
     * Handles dependency change notifications by firing this property's change event.
     *
     * @param observable the dependency observable
     * @param oldValue   ignored (we track converted old/new internally)
     * @param newValue   ignored (we track converted old/new internally)
     */
    @Override
    public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
        fireChanged();
    }

    /**
     * Handles dependency invalidation notifications by firing this property's invalidation event.
     *
     * @param observable the invalidated dependency
     */
    @Override
    public void invalidated(Observable observable) {
        fireInvalidated();
    }

    /**
     * Records property access during closure snooping.
     */
    class Snooper extends GroovyObjectSupport {
        private final Map<String, Snooper> fields = new HashMap<>();

        @Override
        public Object getProperty(String property) {
            Snooper existing = fields.get(property);
            if (existing != null) {
                return existing;
            }
            Snooper created = new Snooper();
            fields.put(property, created);
            return created;
        }

        @Override
        public Object invokeMethod(String name, Object args) {
            return deadEnd;
        }

        /**
         * Returns the map of snooped child property nodes.
         *
         * @return fields map
         */
        public Map<String, Snooper> getFields() {
            return fields;
        }
    }

    /**
     * Thrown when a binding expression attempts to bind to a property on the return
     * value of a method call (unsupported).
     */
    class DeadEndException extends RuntimeException {
        DeadEndException(String message) {
            super(message);
        }
    }

    /**
     * Returned as a placeholder for method invocation results during snooping.
     */
    class DeadEndObject {
        public Object getProperty(String property) {
            throw new DeadEndException("Cannot bind to a property on the return value of a method call");
        }

        public Object invokeMethod(String name, Object args) {
            return this;
        }
    }

    /**
     * Represents a bindable property path (root + children).
     */
    class BindPath {
        private String propertyName;
        private Object currentObject;
        private final List<BindPath> children = new ArrayList<>();
        private ReadOnlyProperty<?> property;

        /**
         * Creates an empty bind path.
         */
        public BindPath() {
        }

        /**
         * Creates a bind path for a single property name.
         *
         * @param propertyName the property name
         */
        public BindPath(String propertyName) {
            this.propertyName = propertyName;
        }

        /**
         * Creates a bind path for a property name and recursively creates children from the snooper tree.
         *
         * @param propertyName the property name
         * @param snooper      snooped property tree
         */
        public BindPath(String propertyName, Snooper snooper) {
            this.propertyName = propertyName;
            createChildren(snooper);
        }

        /**
         * Creates child bind paths for each snooped nested property.
         *
         * @param snooper snooped tree
         */
        public final void createChildren(Snooper snooper) {
            for (Map.Entry<String, Snooper> entry : snooper.getFields().entrySet()) {
                BindPath bp = new BindPath(entry.getKey());
                bp.createChildren(entry.getValue());
                children.add(bp);
            }
        }

        /**
         * Unbinds this path by removing listeners from the underlying JavaFX property.
         */
        public final void unbind() {
            if (property != null) {
                @SuppressWarnings("unchecked")
                ReadOnlyProperty<Object> p = (ReadOnlyProperty<Object>) property;
                p.removeListener((ChangeListener<? super Object>) GroovyClosureProperty.this);
                p.removeListener((InvalidationListener) GroovyClosureProperty.this);
            }
        }

        /**
         * Binds this path: resolves the JavaFX property, attaches listeners, then binds children to
         * the current property's value.
         */
        public final void bind() {
            MetaClass mc = InvokerHelper.getMetaClass(currentObject);

            property = Util.getJavaFXProperty(currentObject, propertyName);

            if (property == null) {
                try {
                    // avoid UndeclaredThrowableException when using closures
                    if (!(currentObject instanceof Closure)) {
                        property = Util.getJavaBeanFXProperty(currentObject, propertyName);
                    }

                    if (property == null) {
                        Object attribute;
                        attribute = mc.getAttribute(currentObject, propertyName);
                        if (attribute instanceof Reference<?> ref) {
                            attribute = ref.get();
                        }
                        property = Util.wrapValueInProperty(attribute);
                    }
                } catch (NoSuchMethodException shouldNotHappen) {
                    shouldNotHappen.printStackTrace();
                }
            }

            @SuppressWarnings("unchecked")
            ReadOnlyProperty<Object> p = (ReadOnlyProperty<Object>) property;
            p.addListener((ChangeListener<? super Object>) GroovyClosureProperty.this);
            p.addListener((InvalidationListener) GroovyClosureProperty.this);

            Object propertyInstance = p.getValue();
            for (BindPath bp : children) {
                bp.setCurrentObject(propertyInstance);
                bp.bind();
            }
        }

        /**
         * @return the property name for this segment
         */
        public final String getPropertyName() {
            return propertyName;
        }

        /**
         * @param propertyName the property name for this segment
         */
        public final void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        /**
         * @return the current object this segment is bound against
         */
        public final Object getCurrentObject() {
            return currentObject;
        }

        /**
         * Sets the current object this segment is bound against.
         *
         * @param currentObject the current object (may be {@code null})
         */
        public final void setCurrentObject(Object currentObject) {
            this.currentObject = currentObject;
        }

        /**
         * @return child segments
         */
        public List<BindPath> getChildren() {
            return children;
        }
    }
}

