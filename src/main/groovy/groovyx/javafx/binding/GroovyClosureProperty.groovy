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


import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Reference;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A JavaFX {@link ReadOnlyProperty} implementation that uses a Groovy Closure as its value supplier,
 * and attempts to automatically bind to the properties referenced by that closure by snooping
 * property access.
 *
 * NOTE: On newer JDKs, reflective access checks must be performed against the closure instance
 * (Field.canAccess(instance)) for instance fields. Passing null will throw IllegalArgumentException.
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

    public GroovyClosureProperty() {
    }

    public GroovyClosureProperty(Closure<?> closure) {
        setClosure(closure);
    }

    public GroovyClosureProperty(Object bean, String name, Closure<?> closure) {
        this.bean = bean;
        this.name = name;
        setClosure(closure);
    }

    public void setClosure(Closure<?> closure) {
        this.closure = closure;
        createBindings(closure);
        fireInvalidated();
    }

    @Override
    public Object getBean() {
        return bean;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addListener(ChangeListener<? super Object> listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    @Override
    public void removeListener(ChangeListener<? super Object> listener) {
        changeListeners.remove(listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        if (listener != null && !invalidationListeners.contains(listener)) {
            invalidationListeners.add(listener);
        }
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    @Override
    public Object getValue() {
        if (valueDirty) {
            update();
        }
        return newValue;
    }

    /**
     * Calculates a new value by evaluating the closure.
     */
    private void update() {
        oldValue = newValue;
        valueDirty = false;

        if (closure != null) {
            try {
                newValue = closure.call();
            } catch (DeadEndException e) {
                // ignore dead-end exceptions from snooping
            } catch (Exception e) {
                // ignore errors from binding evaluation; keep previous value
            }
        }

        if (oldValue != newValue) {
            fireValueChangedEvent();
        }
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

                boolean acc = constructor.canAccess(null);   // OK for constructors
                constructor.setAccessible(true);
                closureLocalCopy = (Closure<?>) constructor.newInstance(args);
                constructor.setAccessible(acc);

                closureLocalCopy.setResolveStrategy(Closure.DELEGATE_ONLY);

                for (Field f : closureClass.getDeclaredFields()) {
                    // JDK 9+ / 25 rule:
                    // - static fields must be checked with canAccess(null)
                    // - instance fields must be checked with canAccess(instance)
                    boolean isStatic = java.lang.reflect.Modifier.isStatic(f.getModifiers())
                    Object accessTarget = isStatic ? null : closureLocalCopy

                    boolean facc = f.canAccess(accessTarget)
                    f.setAccessible(true)
                    try {
                        if (f.getType() == Reference.class) {
                            Object refObj = f.get(closureLocalCopy) // still read from the instance
                            if (refObj instanceof Reference) {
                                def inner = ((Reference<?>) refObj).get()
                                if (inner instanceof Snooper) {
                                    delegate.getFields().put(f.getName(), (Snooper) inner)
                                }
                            }
                        }
                    } finally {
                        f.setAccessible(facc)
                    }
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
     * Fires a change event to any listeners.
     */
    private void fireValueChangedEvent() {
        if (!changeListeners.isEmpty()) {
            ChangeListener<? super Object>[] listeners =
                    changeListeners.toArray(new ChangeListener[0]);
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

    @Override
    public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
        fireInvalidated();
    }

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

            // create nested snooper for chained property access
            Snooper s = new Snooper();
            fields.put(property, s);
            return s;
        }

        @Override
        public Object invokeMethod(String name, Object args) {
            // method invocation while snooping returns a dead-end object so deeper binding fails fast
            return deadEnd;
        }

        public Map<String, Snooper> getFields() {
            return fields;
        }
    }

    /**
     * Raised when attempting to bind through a dead-end (e.g. method return during snooping).
     */
    class DeadEndException extends RuntimeException {
        public DeadEndException() {
            super("Dead end");
        }
    }

    /**
     * Returned from method calls during snooping to prevent illegal binding chains.
     */
    class DeadEndObject {
        @Override
        public String toString() {
            return "<DeadEnd>";
        }
    }

    // ------------------------------------------------------------------------
    // BindPath implementation (unchanged)
    // ------------------------------------------------------------------------

    class BindPath {
        private final List<BindPath> children = new ArrayList<>();
        private String propertyName;
        private Object currentObject;

        public BindPath() {
        }

        public BindPath(String propertyName) {
            this.propertyName = propertyName;
        }

        public BindPath(String propertyName, Snooper snooper) {
            this.propertyName = propertyName;
            if (snooper != null) {
                for (Map.Entry<String, Snooper> entry : snooper.getFields().entrySet()) {
                    BindPath bp = new BindPath(entry.getKey(), entry.getValue());
                    children.add(bp);
                }
            }
        }

        public void setCurrentObject(Object currentObject) {
            this.currentObject = currentObject;
        }

        public Object getCurrentObject() {
            return currentObject;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public List<BindPath> getChildren() {
            return children;
        }

        public void bind() {
            if (currentObject == null || propertyName == null) {
                return;
            }

            Object property = null;

            // Closure root case: allow property access on the real closure to resolve captured vars
            if (currentObject instanceof Closure) {
                try {
                    property = ((Closure<?>) currentObject).getProperty(propertyName);
                } catch (MissingPropertyException ignore) {
                    // fall through
                }
            }

            if (property == null) {
                try {
                    property = Invoker.getProperty(currentObject, propertyName);
                } catch (Exception ignore) {
                    // fall through
                }
            }

            if (property == null) {
                return;
            }

            // If we got a JavaFX Property/ObservableValue, attach listeners and continue down the chain.
            if (property instanceof ReadOnlyProperty || property instanceof ObservableValue) {
                try {
                    @SuppressWarnings("unchecked")
                    ObservableValue<Object> ov = (ObservableValue<Object>) property;
                    ov.addListener((ChangeListener<? super Object>) GroovyClosureProperty.this);
                } catch (ClassCastException ignored) {
                    // ignore
                }

                if (property instanceof Observable) {
                    ((Observable) property).addListener((InvalidationListener) GroovyClosureProperty.this);
                }

                Object propertyInstance = null;
                try {
                    if (property instanceof ReadOnlyProperty) {
                        propertyInstance = ((ReadOnlyProperty<?>) property).getValue();
                    } else if (property instanceof ObservableValue) {
                        propertyInstance = ((ObservableValue<?>) property).getValue();
                    }
                } catch (Exception ignored) {
                    // ignore
                }

                for (BindPath bp : children) {
                    bp.setCurrentObject(propertyInstance);
                    bp.bind();
                }
                return;
            }

            // If it is an ObservableList, treat it as an Observable (invalidation only)
            if (property instanceof ObservableList) {
                ((ObservableList<?>) property).addListener((InvalidationListener) GroovyClosureProperty.this);
                return;
            }

            // Otherwise, if this isn't something we can observe, any deeper chain is a dead-end.
            if (!children.isEmpty()) {
                throw new DeadEndException();
            }
        }
    }

    /**
     * Small helper to avoid Groovy's MetaClass overhead in hot paths.
     */
    static class Invoker {
        static Object getProperty(Object obj, String propertyName) {
            return obj.getClass().getMethod("get" + capitalize(propertyName)).invoke(obj);
        }

        static String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
