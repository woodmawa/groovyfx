package groovyx.javafx

import groovyx.javafx.extension.NodeExtension
import javafx.beans.property.SimpleStringProperty
import javafx.util.Subscription
import spock.lang.Specification

class SubscriptionSpec extends Specification {
    def "test subscription on observable value"() {
        given:
        def property = new SimpleStringProperty("initial")
        def result = ""
        
        when:
        Subscription sub = NodeExtension.subscribe(property) { val ->
            result = val
        }
        property.set("changed")
        
        then:
        result == "changed"
        
        when:
        sub.unsubscribe()
        property.set("changed again")
        
        then:
        result == "changed"
    }

    def "test subscription via SceneGraphBuilder"() {
        given:
        def sg = new SceneGraphBuilder()
        def property = new SimpleStringProperty("initial")
        def result = ""

        when:
        Subscription sub = sg.subscribe(property) { val ->
            result = val
        }
        property.set("changed")

        then:
        result == "changed"

        when:
        sub.unsubscribe()
        property.set("changed again")

        then:
        result == "changed"
    }
}
