package groovyx.javafx.components.skin

import groovyx.javafx.components.Carousel
import groovyx.javafx.components.CarouselSlide
import javafx.animation.FadeTransition
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.SkinBase
import javafx.scene.layout.*
import javafx.util.Duration
import javafx.beans.value.ChangeListener

class CarouselSkin extends SkinBase<Carousel> {

    private final StackPane viewport = new StackPane()
    private final HBox indicators = new HBox(6)

    private final Button prevBtn = new Button("◀")
    private final Button nextBtn = new Button("▶")

    private final ChangeListener<Number> indexListener =
            ({ o, a, b -> render() } as ChangeListener<Number>)

    private final ChangeListener<Boolean> indicatorsListener =
            ({ o, a, b -> updateIndicatorsVisibility() } as ChangeListener<Boolean>)

    private final ChangeListener<Boolean> arrowsListener =
            ({ o, a, b -> updateArrowsVisibility(arrows) } as ChangeListener<Boolean>)

    CarouselSkin(Carousel control) {
        super(control)

        viewport.styleClass.add("carousel-viewport")
        indicators.styleClass.add("carousel-indicators")
        indicators.alignment = Pos.CENTER

        prevBtn.styleClass.add("carousel-prev")
        nextBtn.styleClass.add("carousel-next")

        prevBtn.setOnAction { control.prev() }
        nextBtn.setOnAction { control.next() }

        def overlay = new BorderPane()
        overlay.center = viewport

        // arrows overlay
        def arrows = new BorderPane()
        arrows.left = wrapLeft(prevBtn)
        arrows.right = wrapRight(nextBtn)
        arrows.pickOnBounds = false
        arrows.styleClass.add("carousel-arrows")
        overlay.children.add(arrows)

        // indicators at bottom
        def root = new BorderPane()
        root.center = overlay
        root.bottom = indicators
        BorderPane.setAlignment(indicators, Pos.CENTER)
        BorderPane.setMargin(indicators, new Insets(10, 0, 0, 0))

        getChildren().add(root)


        // listeners
        control.indexProperty.addListener(indexListener)
        control.showIndicatorsProperty.addListener(indicatorsListener)
        control.showArrowsProperty.addListener(arrowsListener)


        rebuildIndicators()
        updateIndicatorsVisibility()
        updateArrowsVisibility(arrows)
        render()
    }

    private Node wrapLeft(Node n) {
        def box = new StackPane(n)
        box.alignment = Pos.CENTER_LEFT
        box.pickOnBounds = false
        return box
    }

    private Node wrapRight(Node n) {
        def box = new StackPane(n)
        box.alignment = Pos.CENTER_RIGHT
        box.pickOnBounds = false
        return box
    }

    private void updateIndicatorsVisibility() {
        indicators.managed = getSkinnable().showIndicators
        indicators.visible = getSkinnable().showIndicators
    }

    private void updateArrowsVisibility(Pane arrowsOverlay) {
        arrowsOverlay.managed = getSkinnable().showArrows
        arrowsOverlay.visible = getSkinnable().showArrows
    }

    private void rebuildIndicators() {
        indicators.children.clear()
        def c = getSkinnable()
        for (int i = 0; i < c.slides.size(); i++) {
            int idx = i
            def dot = new Region()
            dot.styleClass.add("carousel-indicator")
            dot.setOnMouseClicked { c.goTo(idx) }
            indicators.children.add(dot)
        }
        updateIndicatorSelected()
    }

    private void updateIndicatorSelected() {
        int idx = getSkinnable().index
        for (int i = 0; i < indicators.children.size(); i++) {
            def n = indicators.children[i]
            if (i == idx) n.styleClass.add("selected")
            else n.styleClass.remove("selected")
        }
    }

    private void render() {
        def c = getSkinnable()
        viewport.children.clear()

        if (c.slides.isEmpty()) {
            updateIndicatorSelected()
            return
        }

        int idx = Math.max(0, Math.min(c.index, c.slides.size() - 1))
        CarouselSlide slide = c.slides[idx]
        viewport.children.add(slide)

        if (c.animated) {
            animateIn(slide, c.animationDuration)
        }

        updateIndicatorSelected()
    }

    private void animateIn(Node node, Duration d) {
        node.opacity = 0.0
        def ft = new FadeTransition(d, node)
        ft.fromValue = 0.0
        ft.toValue = 1.0
        ft.play()
    }
}
