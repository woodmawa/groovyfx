package groovyx.javafx.appsupport

import groovy.transform.CompileStatic;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCombination;

/**
 * Represents an executable UI action backed by JavaFX properties.
 *
 * <p>An {@code Action} encapsulates common UI action state such as name, description,
 * enabled/selected state, icon identifier, keyboard accelerator, and an action handler.</p>
 *
 * <p>This class is designed to be bound to JavaFX controls such as {@code Button},
 * {@code MenuItem}, {@code ToggleButton}, etc.</p>
 *
 * @author Andres Almiray
 */
@CompileStatic
public class Action {

	private final ObjectProperty<EventHandler<ActionEvent>> onAction =
			new SimpleObjectProperty<>(this, "onAction");

	private final StringProperty name =
			new SimpleStringProperty(this, "name");

	private final StringProperty description =
			new SimpleStringProperty(this, "description");

	private final BooleanProperty enabled =
			new SimpleBooleanProperty(this, "enabled", true);

	private final BooleanProperty selected =
			new SimpleBooleanProperty(this, "selected", false);

	private final StringProperty icon =
			new SimpleStringProperty(this, "icon");

	private final ObjectProperty<KeyCombination> accelerator =
			new SimpleObjectProperty<>(this, "accelerator");

	/**
	 * Creates an empty {@code Action}.
	 */
	public Action() {
		// default constructor
	}

	/**
	 * Returns the JavaFX property holding the {@code onAction} handler.
	 *
	 * @return the onAction property
	 */
	public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
		return onAction;
	}

	/**
	 * Sets the action handler.
	 *
	 * @param value the action handler, or {@code null}
	 */
	public final void setOnAction(EventHandler<ActionEvent> value) {
		onAction.set(value);
	}

	/**
	 * Returns the current action handler.
	 *
	 * @return the action handler, or {@code null}
	 */
	public final EventHandler<ActionEvent> getOnAction() {
		return onAction.get();
	}

	/**
	 * Returns the JavaFX property holding the action name.
	 *
	 * @return the name property
	 */
	public final StringProperty nameProperty() {
		return name;
	}

	/**
	 * Sets the action name.
	 *
	 * @param value the action name
	 */
	public final void setName(String value) {
		name.set(value);
	}

	/**
	 * Returns the action name.
	 *
	 * @return the action name
	 */
	public final String getName() {
		return name.get();
	}

	/**
	 * Returns the JavaFX property holding the action description.
	 *
	 * @return the description property
	 */
	public final StringProperty descriptionProperty() {
		return description;
	}

	/**
	 * Sets the action description.
	 *
	 * @param value the action description
	 */
	public final void setDescription(String value) {
		description.set(value);
	}

	/**
	 * Returns the action description.
	 *
	 * @return the action description
	 */
	public final String getDescription() {
		return description.get();
	}

	/**
	 * Returns the JavaFX property indicating whether the action is enabled.
	 *
	 * @return the enabled property
	 */
	public final BooleanProperty enabledProperty() {
		return enabled;
	}

	/**
	 * Sets whether this action is enabled.
	 *
	 * @param value {@code true} if the action is enabled
	 */
	public final void setEnabled(boolean value) {
		enabled.set(value);
	}

	/**
	 * Returns whether this action is enabled.
	 *
	 * @return {@code true} if the action is enabled
	 */
	public final boolean isEnabled() {
		return enabled.get();
	}

	/**
	 * Returns the JavaFX property indicating whether the action is selected.
	 *
	 * @return the selected property
	 */
	public final BooleanProperty selectedProperty() {
		return selected;
	}

	/**
	 * Sets whether this action is selected.
	 *
	 * @param value {@code true} if the action is selected
	 */
	public final void setSelected(boolean value) {
		selected.set(value);
	}

	/**
	 * Returns whether this action is selected.
	 *
	 * @return {@code true} if the action is selected
	 */
	public final boolean isSelected() {
		return selected.get();
	}

	/**
	 * Returns the JavaFX property holding the icon identifier.
	 *
	 * @return the icon property
	 */
	public final StringProperty iconProperty() {
		return icon;
	}

	/**
	 * Sets the icon identifier (typically a resource path or identifier).
	 *
	 * @param value icon identifier (e.g. resource path)
	 */
	public final void setIcon(String value) {
		icon.set(value);
	}

	/**
	 * Returns the icon identifier.
	 *
	 * @return the icon identifier
	 */
	public final String getIcon() {
		return icon.get();
	}

	/**
	 * Sets the keyboard accelerator.
	 *
	 * @param value key combination or {@code null}
	 */
	public final void setAccelerator(KeyCombination value) {
		accelerator.set(value);
	}

	/**
	 * Returns the JavaFX property holding the keyboard accelerator.
	 *
	 * @return the accelerator property
	 */
	public final ObjectProperty<KeyCombination> acceleratorProperty() {
		return accelerator;
	}

	/**
	 * Returns the keyboard accelerator.
	 *
	 * @return the keyboard accelerator, or {@code null}
	 */
	public final KeyCombination getAccelerator() {
		return accelerator.get();
	}
}
