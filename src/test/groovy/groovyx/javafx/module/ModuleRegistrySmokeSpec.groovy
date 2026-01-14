package groovyx.javafx.module

import groovyx.javafx.appsupport.FxToolkit
import javafx.scene.Node
import javafx.scene.control.Label
import spock.lang.Specification

class ModuleRegistrySmokeSpec extends Specification {

    def "ModuleRegistry can register/get/has and build a module"() {
        given:
        FxToolkit.ensureStarted()
        def reg = new ModuleRegistry()

        expect:
        !reg.has("smoke")

        when:
        reg.register("smoke", new UIModule() {
            @Override
            Node build(Map ctx = [:]) {
                new Label("ok")
            }
        })

        then:
        reg.has("smoke")
        reg.get("smoke") != null

        when:
        def node = reg.get("smoke").build(ModuleContext.of([tag: "registry-smoke"]))

        then:
        node instanceof Label
        ((Label) node).text == "ok"
    }
}
