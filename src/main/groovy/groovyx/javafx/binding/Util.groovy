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

import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import groovy.lang.Script
import groovy.transform.CompileStatic;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanDoublePropertyBuilder;
import javafx.beans.property.adapter.JavaBeanFloatPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanLongPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanBooleanPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanDoublePropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanFloatPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanLongPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanStringPropertyBuilder;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Utility methods used by GroovyFX binding support.
 *
 * <p>These helpers locate JavaFX-style {@code xyzProperty()} accessors, create
 * JavaFX adapter properties for JavaBean-style getters/setters, and wrap plain
 * values in JavaFX {@link Property} instances.</p>
 *
 * @author jimclarke
 */
@CompileStatic
public final class Util {

    private Util() {
        // utility class
    }

    /**
     * Attempts to resolve a JavaFX property accessor named {@code <propertyName>Property}
     * (e.g. {@code textProperty}) on the given instance.
     *
     * @param instance     object that may expose a JavaFX property accessor
     * @param propertyName base property name (without "Property" suffix)
     * @return the resolved JavaFX property, or {@code null} if no accessor exists
     */
    public static ReadOnlyProperty<?> getJavaFXProperty(Object instance, String propertyName) {
        MetaClass mc = InvokerHelper.getMetaClass(instance);
        String fxPropertyAccessor = propertyName + "Property";
        if (!mc.respondsTo(instance, fxPropertyAccessor, (Object[]) null).isEmpty()) {
            return (ReadOnlyProperty<?>) InvokerHelper.invokeMethod(instance, fxPropertyAccessor, (Object[]) null);
        }
        return null;
    }

    /**
     * Wraps a plain value in an appropriate JavaFX {@link Property}. If the value is already a
     * {@link Property}, it is returned as-is.
     *
     * @param value the value to wrap
     * @return a JavaFX property wrapping the given value
     */
    public static Property<?> wrapValueInProperty(Object value) {
        if (value instanceof Property<?> p) {
            return p;
        }
        if (value == null) {
            return new SimpleObjectProperty<>(null);
        }

        Class<?> type = value.getClass();
        if (type == Boolean.class || type == Boolean.TYPE) {
            return new SimpleBooleanProperty((Boolean) value);
        } else if (type == Double.class || type == Double.TYPE) {
            return new SimpleDoubleProperty((Double) value);
        } else if (type == Float.class || type == Float.TYPE) {
            return new SimpleFloatProperty((Float) value);
        } else if (type == Byte.class || type == Byte.TYPE
                || type == Short.class || type == Short.TYPE
                || type == Integer.class || type == Integer.TYPE) {
            return new SimpleIntegerProperty(((Number) value).intValue());
        } else if (type == Long.class || type == Long.TYPE) {
            return new SimpleLongProperty(((Number) value).longValue());
        } else if (type == String.class) {
            return new SimpleStringProperty((String) value);
        } else {
            return new SimpleObjectProperty<>(value);
        }
    }

    /**
     * Determines whether the named JavaBean-style property is writable (i.e. has a setter),
     * or is a script variable.
     *
     * @param instance     the target object (or {@link Script})
     * @param propertyName the property name
     * @return {@code true} if a setter exists or the script variable exists; otherwise {@code false}
     */
    public static boolean isJavaBeanPropertyWritable(Object instance, String propertyName) {
        MetaClass mc = InvokerHelper.getMetaClass(instance);
        MetaProperty metaProperty = mc.getMetaProperty(propertyName);

        if (metaProperty != null) {
            String setterName = MetaProperty.getSetterName(propertyName);
            return !mc.respondsTo(instance, setterName, new Class[]{metaProperty.getType()}).isEmpty();
        } else if (instance instanceof Script script) {
            return script.getProperty(propertyName) != null;
        }

        return false;
    }

    /**
     * Creates a JavaFX adapter property for a JavaBean-style property on the given instance.
     *
     * @param instance     the target object (or {@link Script})
     * @param propertyName the property name
     * @return a read-only or writable JavaFX adapter property, or {@code null} if it cannot be created
     * @throws NoSuchMethodException if the adapter builder cannot resolve required methods
     */
    public static ReadOnlyProperty<?> getJavaBeanFXProperty(Object instance, String propertyName) throws NoSuchMethodException {
        if (isJavaBeanPropertyWritable(instance, propertyName)) {
            return getJavaBeanFXWritableProperty(instance, propertyName);
        }
        return getJavaBeanFXReadOnlyProperty(instance, propertyName);
    }

    /**
     * Creates a read-only JavaFX adapter property for a JavaBean-style property.
     *
     * @param instance     the target object
     * @param propertyName the property name
     * @return a read-only JavaFX adapter property, or {@code null} if it cannot be created
     * @throws NoSuchMethodException if the adapter builder cannot resolve required methods
     */
    public static ReadOnlyProperty<?> getJavaBeanFXReadOnlyProperty(Object instance, String propertyName) throws NoSuchMethodException {
        MetaClass mc = InvokerHelper.getMetaClass(instance);
        MetaProperty metaProperty = mc.getMetaProperty(propertyName);

        if (metaProperty == null) {
            return null;
        }

        Class<?> type = metaProperty.getType();

        if (type == Boolean.class || type == Boolean.TYPE) {
            ReadOnlyJavaBeanBooleanPropertyBuilder builder = ReadOnlyJavaBeanBooleanPropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        } else if (type == Double.class || type == Double.TYPE) {
            ReadOnlyJavaBeanDoublePropertyBuilder builder = ReadOnlyJavaBeanDoublePropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        } else if (type == Float.class || type == Float.TYPE) {
            ReadOnlyJavaBeanFloatPropertyBuilder builder = ReadOnlyJavaBeanFloatPropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        } else if (type == Byte.class || type == Byte.TYPE
                || type == Short.class || type == Short.TYPE
                || type == Integer.class || type == Integer.TYPE) {
            ReadOnlyJavaBeanIntegerPropertyBuilder builder = ReadOnlyJavaBeanIntegerPropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        } else if (type == Long.class || type == Long.TYPE) {
            ReadOnlyJavaBeanLongPropertyBuilder builder = ReadOnlyJavaBeanLongPropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        } else if (type == String.class) {
            ReadOnlyJavaBeanStringPropertyBuilder builder = ReadOnlyJavaBeanStringPropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        } else {
            ReadOnlyJavaBeanObjectPropertyBuilder builder = ReadOnlyJavaBeanObjectPropertyBuilder.create();
            builder.bean(instance).name(propertyName).beanClass(instance.getClass());
            return builder.build();
        }
    }

    /**
     * Creates a writable JavaFX adapter property for a JavaBean-style property.
     *
     * <p>If the instance is a {@link Script} and the binding contains the variable, this will
     * return a {@link ScriptVariableProperty} wrapper.</p>
     *
     * @param instance     the target object (or {@link Script})
     * @param propertyName the property name
     * @return a writable JavaFX adapter property, or {@code null} if it cannot be created
     * @throws NoSuchMethodException if the adapter builder cannot resolve required methods
     */
    public static Property<?> getJavaBeanFXWritableProperty(Object instance, String propertyName) throws NoSuchMethodException {
        MetaClass mc = InvokerHelper.getMetaClass(instance);
        MetaProperty metaProperty = mc.getMetaProperty(propertyName);

        if (metaProperty != null) {
            Class<?> type = metaProperty.getType();

            if (type == Boolean.class || type == Boolean.TYPE) {
                JavaBeanBooleanPropertyBuilder builder = JavaBeanBooleanPropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            } else if (type == Double.class || type == Double.TYPE) {
                JavaBeanDoublePropertyBuilder builder = JavaBeanDoublePropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            } else if (type == Float.class || type == Float.TYPE) {
                JavaBeanFloatPropertyBuilder builder = JavaBeanFloatPropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            } else if (type == Byte.class || type == Byte.TYPE
                    || type == Short.class || type == Short.TYPE
                    || type == Integer.class || type == Integer.TYPE) {
                JavaBeanIntegerPropertyBuilder builder = JavaBeanIntegerPropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            } else if (type == Long.class || type == Long.TYPE) {
                JavaBeanLongPropertyBuilder builder = JavaBeanLongPropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            } else if (type == String.class) {
                JavaBeanStringPropertyBuilder builder = JavaBeanStringPropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            } else {
                JavaBeanObjectPropertyBuilder builder = JavaBeanObjectPropertyBuilder.create();
                builder.bean(instance).name(propertyName).beanClass(instance.getClass());
                return builder.build();
            }
        }

        if (instance instanceof Script script) {
            if (script.getBinding().hasVariable(propertyName)) {
                return ScriptVariableProperty.getProperty(script, propertyName);
            }
            return null;
        }

        return null;
    }
}
