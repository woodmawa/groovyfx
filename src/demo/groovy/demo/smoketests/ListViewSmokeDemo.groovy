import static groovyx.javafx.GroovyFX.start

start {
    stage(title: "GroovyFX ListView Smoke", width: 400, height: 300, visible: true) {
        scene {
            listView(items: ["one", "two", "three"])
        }
    }
}