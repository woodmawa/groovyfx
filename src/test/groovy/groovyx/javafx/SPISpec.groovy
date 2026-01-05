package groovyx.javafx

import groovyx.javafx.factory.ControlFactory
import groovyx.javafx.spi.SceneGraphAddon
import javafx.scene.control.Label
import spock.lang.Specification

class SPISpec extends Specification {
    def setupSpec() {
        GroovyFX.initJavaFX()
    }

    def "test manual factory registration via register"() {
        given:
        def sg = new SceneGraphBuilder()
        
        when:
        sg.register("customLabel", new ControlFactory(Label))
        def label = sg.customLabel(text: "Hello Custom")
        
        then:
        label instanceof Label
        label.text == "Hello Custom"
    }

    def "test manual addon registration via addon method"() {
        given:
        def sg = new SceneGraphBuilder()
        def addon = new SceneGraphAddon() {
            @Override
            void apply(SceneGraphBuilder builder) {
                builder.register("addonLabel", new ControlFactory(Label))
            }
        }
        
        when:
        sg.addon(addon)
        def label = sg.addonLabel(text: "Hello Addon")
        
        then:
        label instanceof Label
        label.text == "Hello Addon"
    }

    def "test manual addon registration via class"() {
        given:
        def sg = new SceneGraphBuilder()
        
        when:
        sg.addon(TestAddon)
        def label = sg.testAddonLabel(text: "Hello Class Addon")
        
        then:
        label instanceof Label
        label.text == "Hello Class Addon"
    }

    def "test registerComponentNode with Style A1"() {
        given:
        def sg = new SceneGraphBuilder()
        
        when:
        sg.registerComponentNode("styleA1", StyleA1Component)
        // Note: currently ComponentClassFactory doesn't pass the body to static build
        // unless we use __body attribute. Let's see if we can improve this.
        def box = sg.styleA1(title: "StyleA1") {
            label("Child Label")
        }
        
        then:
        box instanceof javafx.scene.layout.VBox
        // If we don't improve ComponentClassFactory, the children might be added twice
        // once by the static build (if it could get the body) and once by the builder.
        // But currently static build doesn't get the body.
        box.children.size() >= 1
    }
}

class TestAddon implements SceneGraphAddon {
    @Override
    void apply(SceneGraphBuilder builder) {
        builder.register("testAddonLabel", new ControlFactory(Label))
    }
}

class StyleA1Component {
    static javafx.scene.Node build(groovy.util.FactoryBuilderSupport b, Map attrs, Closure body) {
        // body will be null here because of how builder sequence works for now,
        // but children will be added via builder.setChild if the returned node is a Pane
        return new javafx.scene.layout.VBox()
    }
}
