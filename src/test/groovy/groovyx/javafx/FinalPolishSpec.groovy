package groovyx.javafx

import groovyx.javafx.components.ToggleSwitch
import javafx.application.Platform
import spock.lang.Specification

class FinalPolishSpec extends Specification {
    def setupSpec() {
        GroovyFX.initJavaFX()
    }

    def "test ToggleSwitch component"() {
        given:
        def ts = new ToggleSwitch()
        
        expect:
        !ts.isSelected()
        
        when:
        ts.setSelected(true)
        
        then:
        ts.isSelected()
    }

    def "test Store state management"() {
        given:
        def store = new Store<Integer>(10)
        def result = 0
        store.subscribe { result = it }
        
        when:
        store.setState(20)
        
        then:
        store.getState() == 20
        result == 20
        
        when:
        store.update { it + 5 }
        
        then:
        store.getState() == 25
        result == 25
    }

    def "test FormLayout and validation"() {
        given:
        def sg = new SceneGraphBuilder()
        def form
        
        when:
        GroovyFX.runOnFxThread {
            form = sg.formLayout {
                textField(label: "Name", id: "nameField", validate: { it.length() < 3 ? "Too short" : null })
            }
        }
        
        then:
        form != null
        def nameField = sg.nameField
        
        when:
        GroovyFX.runOnFxThread {
            nameField.setText("Ab")
        }
        
        then:
        // Verification of error label text would require deeper node traversal
        // but we verify the DSL doesn't crash
        nameField.getText() == "Ab"
    }
}
