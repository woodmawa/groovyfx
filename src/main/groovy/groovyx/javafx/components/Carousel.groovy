package groovyx.javafx.components

import groovyx.javafx.components.skin.CarouselSkin
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.util.Duration

class Carousel extends Control {

    final ObservableList<CarouselSlide> slides = FXCollections.observableArrayList()

    final IntegerProperty indexProperty = new SimpleIntegerProperty(this, "index", 0)
    final BooleanProperty wrapProperty = new SimpleBooleanProperty(this, "wrap", true)

    final BooleanProperty showIndicatorsProperty = new SimpleBooleanProperty(this, "showIndicators", true)
    final BooleanProperty showArrowsProperty = new SimpleBooleanProperty(this, "showArrows", true)

    final BooleanProperty animatedProperty = new SimpleBooleanProperty(this, "animated", true)
    final ObjectProperty<Duration> animationDurationProperty =
            new SimpleObjectProperty<>(this, "animationDuration", Duration.millis(180))


    private final ListChangeListener<CarouselSlide> slidesListener =
            { change -> clampIndex() } as ListChangeListener<CarouselSlide>

    Carousel() {
        styleClass.add("carousel")

        // clamp index when slides change
        slides.addListener(slidesListener)
        indexProperty.addListener(({ obs, oldV, newV ->
            clampIndex()
        } as ChangeListener<Number>))
    }

    int getIndex() { indexProperty.get() }
    void setIndex(int v) { indexProperty.set(v) }

    boolean isWrap() { wrapProperty.get() }
    void setWrap(boolean v) { wrapProperty.set(v) }

    boolean isShowIndicators() { showIndicatorsProperty.get() }
    void setShowIndicators(boolean v) { showIndicatorsProperty.set(v) }

    boolean isShowArrows() { showArrowsProperty.get() }
    void setShowArrows(boolean v) { showArrowsProperty.set(v) }

    boolean isAnimated() { animatedProperty.get() }
    void setAnimated(boolean v) { animatedProperty.set(v) }

    Duration getAnimationDuration() { animationDurationProperty.get() }
    void setAnimationDuration(Duration d) { animationDurationProperty.set(d) }

    void next() {
        if (slides.isEmpty()) return
        int i = getIndex() + 1
        if (i >= slides.size()) setIndex(isWrap() ? 0 : slides.size() - 1)
        else setIndex(i)
    }

    void prev() {
        if (slides.isEmpty()) return
        int i = getIndex() - 1
        if (i < 0) setIndex(isWrap() ? slides.size() - 1 : 0)
        else setIndex(i)
    }

    void goTo(int i) { setIndex(i) }

    private void clampIndex() {
        if (slides.isEmpty()) {
            if (getIndex() != 0) setIndex(0)
            return
        }
        int i = getIndex()
        if (i < 0) setIndex(0)
        else if (i >= slides.size()) setIndex(slides.size() - 1)
    }

    @Override
    protected Skin createDefaultSkin() {
        return new CarouselSkin(this)
    }
}
