package demo.components

import groovyx.javafx.GroovyFX
import groovyx.javafx.components.Carousel
import javafx.geometry.Insets
import javafx.geometry.Pos

new GroovyFX().start {

    stage(
            title: "Carousel (Core Component Demo)",
            width: 720,
            height: 460,
            visible: true
    ) {
        scene {
            vbox(
                    spacing: 12,
                    padding: new Insets(16),
                    alignment: Pos.CENTER
            ) {

                def carousel = carousel(
                        index: 0,
                        wrap: true,
                        animated: true,
                        showIndicators: true,
                        showArrows: true
                ) {
                    slide {
                        label(
                                "Slide 1",
                                style: "-fx-font-size: 28px; -fx-padding: 36; " +
                                        "-fx-background-color: whitesmoke; -fx-border-color: #ccc;"
                        )
                    }

                    slide {
                        vbox(spacing: 8, alignment: Pos.CENTER) {
                            label("Slide 2", style: "-fx-font-size: 24px;")
                            button("Action")
                        }
                        style = "-fx-padding: 36; -fx-background-color: aliceblue; -fx-border-color: #ccc;"
                    }

                    slide {
                        label(
                                "Slide 3",
                                style: "-fx-font-size: 28px; -fx-padding: 36; " +
                                        "-fx-background-color: honeydew; -fx-border-color: #ccc;"
                        )
                    }
                } as Carousel

                // Status label using bindText (1-based index)
                label(
                        bindText: carousel.indexProperty.add(1)
                                .asString("Slide %d of ${carousel.slides.size()}"),
                        style: "-fx-text-fill: #444;"
                )

                hbox(spacing: 10, alignment: Pos.CENTER) {
                    button("◀ Prev", onAction: { carousel.prev() })
                    button("Next ▶", onAction: { carousel.next() })
                }

                // Live property toggles
                hbox(spacing: 14, alignment: Pos.CENTER) {
                    def wrapBox = checkBox("Wrap", selected: carousel.wrap)
                    wrapBox.selectedProperty().addListener { _, _, v ->
                        carousel.wrap = (boolean) v
                    }

                    def arrowsBox = checkBox("Arrows", selected: carousel.showArrows)
                    arrowsBox.selectedProperty().addListener { _, _, v ->
                        carousel.showArrows = (boolean) v
                    }

                    def indicatorsBox = checkBox("Indicators", selected: carousel.showIndicators)
                    indicatorsBox.selectedProperty().addListener { _, _, v ->
                        carousel.showIndicators = (boolean) v
                    }
                }
            }
        }
    }
}
