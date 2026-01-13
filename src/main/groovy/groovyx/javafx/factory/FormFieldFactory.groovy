package groovyx.javafx.factory

import groovy.util.AbstractFactory
import groovy.util.FactoryBuilderSupport
import groovyx.javafx.components.FormField
import groovyx.javafx.components.FormLayout
import javafx.scene.Node
import javafx.scene.control.Label

/**
 * Builds a FormField wrapper and captures exactly one Node as its content.
 */
class FormFieldFactory extends AbstractFXBeanFactory {

    FormFieldFactory() {
        super(FormField, false) // leaf=false so field { } works
    }

    @Override
    boolean isLeaf() { false }

    @Override
    boolean isHandlesNodeChildren() { true }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attrs) {
        // Create ourselves so we can *reliably* set label/validate
        def ff = new FormField()
        ff.label = attrs.remove('label')
        ff.validate = attrs.remove('validate')

        return ff
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (parent instanceof FormField && child instanceof Node) {
            parent.content = (Node) child
        }
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        def ff = (FormField) node

        if (!(parent instanceof FormLayout)) return
        FormLayout form = (FormLayout) parent

        if (!ff.label || !(ff.content instanceof Node)) {
            return
        }

        Node control = (Node) ff.content
        Closure validateClosure = (Closure) ff.validate

        if (!validateClosure) {
            form.addField(ff.label, control)                 // <-- use the visible-node overload
            form.requestLayout()
            return
        }

        Label errorLabel = new Label()
        errorLabel.managed = true
        errorLabel.visible = true
        errorLabel.styleClass.add("form-error")

        form.addField(ff.label, control, errorLabel)         // <-- same as your factory used to do
        form.requestLayout()

        Closure updateError = { Object v ->
            String err = (String) validateClosure.call(v)
            errorLabel.text = err ?: ""
        }

        // Wire validation
        if (control.metaClass.respondsTo(control, "textProperty")) {
            control.textProperty().addListener({ obs, oldV, newV -> updateError(newV) } as javafx.beans.value.ChangeListener)
            if (control.metaClass.respondsTo(control, "getText")) updateError(control.text)
            else updateError(null)
        } else if (control.metaClass.respondsTo(control, "valueProperty")) {
            control.valueProperty().addListener({ obs, oldV, newV -> updateError(newV) } as javafx.beans.value.ChangeListener)
            if (control.metaClass.respondsTo(control, "getValue")) updateError(control.value)
            else updateError(null)
        } else if (control.metaClass.respondsTo(control, "selectedProperty")) {
            control.selectedProperty().addListener({ obs, oldV, newV -> updateError(newV) } as javafx.beans.value.ChangeListener)
            if (control.metaClass.respondsTo(control, "isSelected")) updateError(control.selected)
            else updateError(null)
        } else {
            updateError(null)
        }
    }
}