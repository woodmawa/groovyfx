package groovyx.javafx.binding;

import groovy.lang.Binding;
import groovy.lang.Script;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides JavaFX {@link Property} wrappers for Groovy {@link Script} variables.
 *
 * <p>This class maintains a per-script cache of properties so that repeated requests
 * for the same script variable return the same JavaFX property instance.</p>
 *
 * <p>The script's {@link Binding} variables map is wrapped in an {@link ObservableMap}
 * so that changes to script variables can be observed and propagated to the cached
 * JavaFX properties.</p>
 *
 * @author jimclarke
 */
public class ScriptVariableProperty implements MapChangeListener<String, Object> {

    private static final ScriptVariableProperty INSTANCE = new ScriptVariableProperty();
    private static final String SCRIPT_VAR = "__script__";

    /**
     * Stores JavaFX properties for each script instance.
     * Keyed first by script instance, then by variable name.
     */
    private static final Map<Script, Map<String, Property<?>>> propertyMap = new HashMap<>();

    /**
     * Returns a JavaFX {@link Property} that reflects the value of the given script variable.
     *
     * <p>If a property for the given script and variable already exists, it is returned.
     * Otherwise, a new property is created, cached, and returned.</p>
     *
     * <p>On first access for a script, the script's binding variables map is wrapped in an
     * {@link ObservableMap} and a listener is attached to propagate changes.</p>
     *
     * @param script       the Groovy script instance
     * @param propertyName the name of the script variable
     * @return a JavaFX property reflecting the script variable
     */
    public static Property<?> getProperty(Script script, String propertyName) {
        Map<String, Property<?>> instanceMap = propertyMap.get(script);
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
            propertyMap.put(script, instanceMap);

            // Binding.getVariables() is raw in Groovy; we wrap it carefully.
            @SuppressWarnings({"unchecked", "rawtypes"})
            Map<String, Object> originalVMap = (Map) script.getBinding().getVariables();

            originalVMap.put(SCRIPT_VAR, script);

            ObservableMap<String, Object> obsVariables = FXCollections.observableMap(originalVMap);
            obsVariables.addListener(INSTANCE);

            Binding newBinding = new Binding(obsVariables);
            script.setBinding(newBinding);
        }

        Property<?> property = instanceMap.get(propertyName);
        if (property == null) {
            Object value = script.getProperty(propertyName);
            Class<?> type = (value != null) ? value.getClass() : Object.class;

            // Script values are always boxed types (never primitives), so no *.TYPE checks needed.
            if (type == Boolean.class) {
                property = new ScriptVariableBooleanProperty(script, propertyName);
            } else if (type == BigDecimal.class || type == Double.class) {
                property = new ScriptVariableDoubleProperty(script, propertyName);
            } else if (type == Float.class) {
                property = new ScriptVariableFloatProperty(script, propertyName);
            } else if (type == Byte.class || type == Short.class || type == Integer.class) {
                property = new ScriptVariableIntegerProperty(script, propertyName);
            } else if (type == BigInteger.class || type == Long.class) {
                property = new ScriptVariableLongProperty(script, propertyName);
            } else if (type == String.class) {
                property = new ScriptVariableStringProperty(script, propertyName);
            } else {
                property = new ScriptVariableObjectProperty<>(script, propertyName);
            }

            instanceMap.put(propertyName, property);
        }

        return property;
    }

    /**
     * Private constructor; this class is used as a singleton listener instance.
     */
    private ScriptVariableProperty() {
        // singleton
    }

    /**
     * Called when a script binding variable changes.
     *
     * <p>If a JavaFX property exists for the changed variable, its value is updated to match
     * the new value in the script variables map.</p>
     *
     * @param change the map change event
     */
    @Override
    public void onChanged(Change<? extends String, ? extends Object> change) {
        ObservableMap<? extends String, ? extends Object> map = change.getMap();

        Script script = (Script) map.get(SCRIPT_VAR);
        if (script == null) return;

        Map<String, Property<?>> instanceMap = propertyMap.get(script);
        if (instanceMap == null) return;

        String variable = change.getKey();
        Property<?> property = instanceMap.get(variable);
        if (property == null) return;

        // Script variables are dynamic (Object). Bridge wildcard capture via a single controlled cast.
        @SuppressWarnings({"rawtypes", "unchecked"})
        Property raw = (Property) property;
        raw.setValue(map.get(variable));
    }
}
