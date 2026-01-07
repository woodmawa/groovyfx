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

import groovy.lang.Script
import groovy.transform.CompileStatic;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Wraps a Groovy {@link Script} variable in a JavaFX {@link SimpleObjectProperty}.
 *
 * <p>The underlying script property is updated whenever this JavaFX property is set.</p>
 *
 * @param <T> value type
 * @author jimclarke
 */
@CompileStatic
public class ScriptVariableObjectProperty<T> extends SimpleObjectProperty<T> {

    /**
     * Creates a property bound to a variable on the given {@link Script}.
     *
     * @param script       the owning script (used as this property's bean)
     * @param propertyName the script variable/property name (used as this property's name)
     */
    public ScriptVariableObjectProperty(Script script, String propertyName) {
        super(script, propertyName, readScriptValue(script, propertyName));
    }

    private static <T> T readScriptValue(Script script, String propertyName) {
        @SuppressWarnings("unchecked")
        T value = (T) script.getProperty(propertyName);
        return value;
    }

    /**
     * Sets the value of this JavaFX property and mirrors it onto the backing script variable.
     *
     * @param newValue the new value
     */
    @Override
    public void set(T newValue) {
        // Keep the script variable in sync with the JavaFX property.
        ((Script) getBean()).setProperty(getName(), newValue);
        super.set(newValue);
    }
}
