package groovyx.javafx

import groovyx.javafx.components.Icon
import groovyx.javafx.components.ResponsivePane
import javafx.scene.paint.Color
import spock.lang.Specification

class NewGapsSpec extends Specification {
    def setupSpec() {
        GroovyFX.initJavaFX()
    }

    def "test Icon component"() {
        given:
        def sg = new SceneGraphBuilder()
        Icon icon
        
        when:
        icon = sg.icon(iconName: "save", fill: Color.RED)
        
        then:
        icon != null
        icon.getIconName() == "save"
        icon.getFill() == Color.RED
    }

    def "test ResponsivePane component"() {
        given:
        def sg = new SceneGraphBuilder()
        ResponsivePane rp
        
        when:
        rp = sg.responsivePane(breakpoint: 500) {
            button("B1")
            button("B2")
        }
        
        then:
        rp != null
        rp.getBreakpoint() == 500
        rp.getChildren().size() == 2
    }
}
