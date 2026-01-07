/*
 * SPDX-License-Identifier: Apache-2.0
 */
package examples


import groovyx.javafx.Store
import static groovyfx.javafx.GroovyFX.start
import javafx.scene.paint.Color

/**
 * Modern Dashboard Example
 * Demonstrates: Cards, Badges, Icons, Reactive Store, and modern layout.
 */

// Define a reactive state store for the dashboard
def counterStore = new Store<Integer>(0)

start {
    stage(title: 'GroovyFX Modern Dashboard', width: 800, height: 600, show: true) {
        scene(fill: Color.WHITESMOKE) {
            vbox(spacing: 20, padding: 25) {
                // Header Area
                hbox(spacing: 10, alignment: 'CENTER_LEFT') {
                    icon(iconName: 'info', fill: Color.DODGERBLUE, scaleX: 1.5, scaleY: 1.5)
                    label('System Dashboard', style: '-fx-font-size: 24px; -fx-font-weight: bold;')
                    region(hgrow: 'ALWAYS')
                    badge(text: 'v2.0.0', backgroundFill: Color.MEDIUMSEAGREEN)
                }

                // Summary Cards
                hbox(spacing: 20) {
                    card(hgrow: 'ALWAYS') {
                        cardHeader { label('Active Users', style: '-fx-font-weight: bold;') }
                        cardBody { 
                            hbox(alignment: 'CENTER', spacing: 10) {
                                icon(iconName: 'edit', fill: Color.GRAY)
                                label('1,234', style: '-fx-font-size: 32px;') 
                            }
                        }
                        cardFooter { label('Up 5% from yesterday', style: '-fx-text-fill: green;') }
                    }

                    card(hgrow: 'ALWAYS') {
                        cardHeader { label('System Tasks', style: '-fx-font-weight: bold;') }
                        cardBody {
                            vbox(alignment: 'CENTER', spacing: 5) {
                                label(id: 'counterLabel', text: "Tasks: ${counterStore.state}", style: '-fx-font-size: 32px;')
                                // Subscribe label to store updates
                                counterStore.subscribe { val ->
                                    counterLabel.text = "Tasks: ${val}"
                                }
                            }
                        }
                        cardFooter { 
                            hbox(spacing: 10) {
                                button('Add Task', onAction: { counterStore.update { it + 1 } })
                                button('Reset', onAction: { counterStore.setState(0) })
                            }
                        }
                    }
                }

                // Detailed Table with Auto-Columns
                def sampleData = [
                    [id: 1, name: 'Backup Job', status: 'Completed', time: '10:00 AM'],
                    [id: 2, name: 'Security Scan', status: 'In Progress', time: '10:15 AM'],
                    [id: 3, name: 'Update Check', status: 'Pending', time: '10:30 AM']
                ]

                card(vgrow: 'ALWAYS') {
                    cardHeader { label('Recent Activity Log', style: '-fx-font-weight: bold;') }
                    cardBody {
                        tableView(items: sampleData, autoColumns: sampleData, vgrow: 'ALWAYS')
                    }
                }
                
                button('Show Notification', onAction: { notify("Dashboard Refresh Successful!") })
            }
        }
    }
}
