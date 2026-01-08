/*
 * SPDX-License-Identifier: Apache-2.0
 */
package examples


import static groovyx.javafx.GroovyFX.start
import javafx.scene.paint.Color

/**
 * Responsive Form Example
 * Demonstrates: FormLayout, Validation, ToggleSwitch, ResponsivePane, and Toasts.
 */

start {
    stage(title: 'GroovyFX User Profile', width: 500, height: 650, show: true) {
        scene(fill: Color.WHITE) {
            vbox(spacing: 20, padding: 30) {
                hbox(spacing: 15, alignment: 'CENTER_LEFT') {
                    icon(iconName: 'edit', fill: Color.DARKSLATEBLUE, scaleX: 1.2, scaleY: 1.2)
                    label('User Profile Settings', style: '-fx-font-size: 20px; -fx-font-weight: bold;')
                }

                responsivePane(breakpoint: 400, hgrow: 'ALWAYS') {
                    // This section will stack on narrow windows
                    formLayout(responsive: true, hgrow: 'ALWAYS') {
                        textField(label: 'First Name', text: 'John', validate: { it.isEmpty() ? 'Cannot be empty' : null })
                        textField(label: 'Last Name', text: 'Doe', validate: { it.isEmpty() ? 'Cannot be empty' : null })
                        textField(label: 'Email', text: 'john.doe@example.com', validate: { it.contains('@') ? null : 'Invalid email' })
                        
                        hbox(label: 'Newsletter') {
                            toggleSwitch(id: 'newsToggle', selected: true)
                            label(' Subscribe to weekly updates')
                        }

                        colorPicker(label: 'Theme Color', value: Color.DODGERBLUE)
                        
                        passwordField(label: 'Secret Key', validate: { it.length() < 8 ? 'Min 8 characters' : null })
                    }
                }

                region(vgrow: 'ALWAYS')

                hbox(spacing: 10, alignment: 'CENTER_RIGHT') {
                    button('Cancel', style: '-fx-base: #eeeeee;', onAction: { notify("Changes discarded") })
                    button('Save Changes', style: '-fx-base: #4caf50; -fx-text-fill: white;', onAction: { 
                        notify("Profile saved successfully!", duration: 5.s) 
                    })
                }
            }
        }
    }
}
