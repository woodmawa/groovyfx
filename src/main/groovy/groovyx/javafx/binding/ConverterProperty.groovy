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
import groovy.lang.GString
import groovy.transform.CompileStatic;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ObservableValue} that wraps another {@link ObservableValue} and applies a Groovy
 * {@link Closure} to convert the base value into a derived value.
 *
 * <p>The converted value is cached, and change/invalidation events from the base property are
 * forwarded to listeners registered on this instance.</p>
 *
 * <p>If the converter returns a {@link GString}, it is coerced to a {@link String} to avoid
 * leaking Groovy-specific string types into JavaFX binding consumers.</p>
 *
 * @author jimclarke
 */
@CompileStatic
public class ConverterProperty implements ObservableValue<Object>,
        ChangeListener<Object>,
        InvalidationListener {

    private final ObservableValue<?> baseProperty;
    private final Closure<?> converter;

    private final List<ChangeListener<? super Object>> changeListeners = new ArrayList<>();
    private final List<InvalidationListener> invalidationListeners = new ArrayList<>();

    private Object oldValue = null;
    private Object newValue = null;

    /**
     * Creates a new {@code ConverterProperty} that derives its value from {@code baseProperty}
     * by applying {@code converter}.
     *
     * @param baseProperty the base observable value to listen to
     * @param converter    a closure that converts the base value into the derived value
     */
    public ConverterProperty(ObservableValue<?> baseProperty, Closure<?> converter) {
        this.baseProperty = baseProperty;
        this.converter = converter;

        // Listen to both change and invalidation on the base property.
        // We intentionally bridge the wildcard types to Object here.
        @SuppressWarnings("unchecked")
        ObservableValue<Object> asObject = (ObservableValue<Object>) baseProperty;

        asObject.addListener((ChangeListener<Object>) this);
        baseProperty.addListener((InvalidationListener) this);

        // Initialize cached value
        getValue();
    }

    /**
     * Returns the current converted value, updating the cached old/new values.
     *
     * @return the converted value
     */
    @Override
    public final Object getValue() {
        oldValue = newValue;
        newValue = converter.call(baseProperty.getValue());
        if (newValue instanceof GString) {
            newValue = newValue.toString();
        }
        return newValue;
    }

    /**
     * Adds a {@link ChangeListener} to this converted observable.
     *
     * @param listener the listener to add
     */
    @Override
    public void addListener(ChangeListener<? super Object> listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes a {@link ChangeListener} from this converted observable.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(ChangeListener<? super Object> listener) {
        changeListeners.remove(listener);
    }

    /**
     * Adds an {@link InvalidationListener} to this converted observable.
     *
     * @param listener the listener to add
     */
    @Override
    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    /**
     * Removes an {@link InvalidationListener} from this converted observable.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    /**
     * Called when the base property changes. Recomputes the converted value and notifies
     * all registered change listeners with the cached old/new converted values.
     *
     * @param obs    the observed value (base property)
     * @param oldVal the previous base value (ignored; we track converted oldValue)
     * @param newVal the new base value (ignored; we track converted newValue)
     */
    @Override
    public void changed(ObservableValue<? extends Object> obs, Object oldVal, Object newVal) {
        getValue();

        if (!changeListeners.isEmpty()) {
            ChangeListener<? super Object>[] listeners =
                    changeListeners.toArray(new ChangeListener[0]);
            for (ChangeListener<? super Object> l : listeners) {
                l.changed(this, oldValue, newValue);
            }
        }
    }

    /**
     * Called when the base property is invalidated. Forwards invalidation to all listeners
     * registered on this instance.
     *
     * @param obs the invalidated observable (base property)
     */
    @Override
    public void invalidated(Observable obs) {
        if (!invalidationListeners.isEmpty()) {
            InvalidationListener[] listeners =
                    invalidationListeners.toArray(new InvalidationListener[0]);
            for (InvalidationListener l : listeners) {
                l.invalidated(this);
            }
        }
    }
}
