package groovyx.javafx.components

import groovyx.javafx.components.skin.RibbonSkin
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.collections.ListChangeListener
import javafx.scene.Node


class Ribbon extends Control {

    static enum DisplayMode { AUTO_HIDE, SHOW_TABS_ONLY, ALWAYS_SHOW }

    private final ObservableList<RibbonTab> tabs = FXCollections.observableArrayList()
    private final ObjectProperty<RibbonTab> selectedTab = new SimpleObjectProperty<>(this, "selectedTab")
    private final ObjectProperty<DisplayMode> displayMode =
            new SimpleObjectProperty<>(this, "displayMode", DisplayMode.ALWAYS_SHOW)

    private final BooleanProperty collapsible =
            new SimpleBooleanProperty(this, "collapsible", true)

    private final BooleanProperty collapsed =
            new SimpleBooleanProperty(this, "collapsed", false)

    ObjectProperty<Node> quickAccess =
            new SimpleObjectProperty<>(this, "quickAccess")

    private final ObjectProperty<Node> backstage = new SimpleObjectProperty<>(this, "backstage")
    ObjectProperty<Node> backstageProperty() { backstage }
    Node getBackstage() { backstage.get() }
    void setBackstage(Node n) { backstage.set(n) }
    void backstage(Node node) { setBackstage(node) }

    Ribbon() {
        getStyleClass().add("ribbon")

        // keep collapsed in sync with displayMode (v1 behavior)
        displayMode.addListener { _, _, m ->
            switch (m) {
                case DisplayMode.ALWAYS_SHOW:
                    setCollapsed(false)
                    break
                case DisplayMode.SHOW_TABS_ONLY:
                case DisplayMode.AUTO_HIDE:
                    // v1: auto-hide behaves like tabs-only
                    setCollapsed(true)
                    break
            }
        }

        // if collapsible turned off, force always show
        collapsible.addListener { _, _, v ->
            if (!v) {
                setDisplayMode(DisplayMode.ALWAYS_SHOW)
                setCollapsed(false)
            }
        }

        // if first tab added and none selected, select it
        tabs.addListener(({ ListChangeListener.Change<? extends RibbonTab> change ->
            while (change.next()) {
                if (change.wasAdded() && getSelectedTab() == null && !tabs.isEmpty()) {
                    setSelectedTab(tabs.get(0))
                }
            }
        } as ListChangeListener<RibbonTab>))
    }

    ObjectProperty<Node> quickAccessProperty() { quickAccess }
    Node getQuickAccess() { quickAccess.get() }
    void setQuickAccess(Node n) { quickAccess.set(n) }

    ObservableList<RibbonTab> getTabs() { tabs }

    ObjectProperty<RibbonTab> selectedTabProperty() { selectedTab }
    RibbonTab getSelectedTab() { selectedTab.get() }
    void setSelectedTab(RibbonTab t) { selectedTab.set(t) }

    ObjectProperty<DisplayMode> displayModeProperty() { displayMode }
    DisplayMode getDisplayMode() { displayMode.get() }
    void setDisplayMode(DisplayMode m) { displayMode.set(m) }

    BooleanProperty collapsibleProperty() { collapsible }
    boolean isCollapsible() { collapsible.get() }
    void setCollapsible(boolean v) { collapsible.set(v) }

    BooleanProperty collapsedProperty() { collapsed }
    boolean isCollapsed() { collapsed.get() }
    void setCollapsed(boolean v) { collapsed.set(v) }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new RibbonSkin(this)
    }

    void quickAccess(Node node) {
        setQuickAccess(node)
    }
}
