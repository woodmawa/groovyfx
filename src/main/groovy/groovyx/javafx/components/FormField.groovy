package groovyx.javafx.components

import javafx.scene.Node

/**
 * Wrapper node for FormLayout rows.
 *
 * Used by the DSL:
 *   field(label: "Name", validate: { ... }) { textField(...) }
 */
class FormField {
    String label
    Closure validate
    Node content
}