package groovyx.javafx

import groovyx.javafx.components.Badge
import groovyx.javafx.components.Card
import javafx.scene.paint.Color
import spock.lang.Specification

class ModernComponentsSpec extends Specification {
    def "test card component DSL"() {
        given:
        GroovyFX.initJavaFX()
        def sg = new SceneGraphBuilder()
        
        when:
        Card card = sg.card {
            cardHeader {
                label("Header")
            }
            cardBody {
                label("Body Content")
            }
            cardFooter {
                button("Action")
            }
        }
        
        then:
        card != null
        card.getHeaderContainer().getChildren().size() == 1
        card.getBodyContainer().getChildren().size() == 1
        card.getFooterContainer().getChildren().size() == 1
    }

    def "test badge component"() {
        given:
        GroovyFX.initJavaFX()
        def sg = new SceneGraphBuilder()
        
        when:
        Badge badge = sg.badge(text: "New", backgroundFill: Color.RED)
        
        then:
        badge != null
        badge.getText() == "New"
    }

    def "test missing standard components are registered"() {
        given:
        GroovyFX.initJavaFX()
        def sg = new SceneGraphBuilder()
        
        expect:
        sg.colorPicker() != null
        sg.pagination() != null
    }
}
