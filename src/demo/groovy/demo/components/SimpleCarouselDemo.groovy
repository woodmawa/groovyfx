package demo.components

import demo.components.carousel.SimpleCarousel
import groovyx.javafx.GroovyFX
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {
    stage(title: "SimpleCarousel SPI Demo", width: 700, height: 420, visible: true) {
        scene {
            vbox(spacing: 12, padding: new Insets(16), alignment: Pos.CENTER) {

                def c = simpleCarousel(wrap: true, index: 0) {
                    label("Slide 1", style: "-fx-font-size: 26px; -fx-padding: 36; -fx-background-color: whitesmoke; -fx-border-color: lightgray;")
                    label("Slide 2", style: "-fx-font-size: 26px; -fx-padding: 36; -fx-background-color: aliceblue; -fx-border-color: lightgray;")
                    label("Slide 3", style: "-fx-font-size: 26px; -fx-padding: 36; -fx-background-color: honeydew; -fx-border-color: lightgray;")
                } as SimpleCarousel

                // Create the label and keep a reference to the real Label instance
                // Create the label and keep a reference to the real Label instance
                def status = label(
                        bindText: c.indexProperty.add(1).asString("Slide %d of 3"),
                        style: "-fx-text-fill: #444;"
                )


                hbox(spacing: 10, alignment: Pos.CENTER) {
                    button("◀ Prev", onAction: { c.prev() })
                    button("Next ▶", onAction: { c.next() })
                }

                // Optional: prove wrap is a live property (no binding needed)
                hbox(spacing: 12, alignment: Pos.CENTER) {
                    def wrapBox = checkBox("Wrap", selected: c.wrap)
                    wrapBox.selectedProperty().addListener { _, _, v -> c.wrap = (boolean) v }
                }
            }
        }
    }
}
