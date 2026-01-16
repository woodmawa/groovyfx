package groovyx.javafx.components.skin

import groovyx.javafx.components.Ribbon
import groovyx.javafx.components.RibbonGroup
import groovyx.javafx.components.RibbonTab
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.control.SkinBase
import javafx.scene.layout.*

class RibbonSkin extends SkinBase<Ribbon> {

    // ----- Header: left (backstage + quick access) + center (tabs) -----
    private final HBox leftBox = new HBox(6)

    private final HBox tabHeader = new HBox(6)
    private final ToggleGroup tabToggleGroup = new ToggleGroup()

    // Reused header-center container + separator (avoid recreating nodes each rebuild)
    private final HBox centerBox = new HBox(0)
    private final Separator headerSep = new Separator(Orientation.VERTICAL)

    private final BorderPane headerRow = new BorderPane()
    private final VBox headerBox = new VBox()

    // ----- Strip: groups row + display options -----
    private final StackPane contentPane = new StackPane()
    private final BorderPane stripRow = new BorderPane()
    private final HBox groupsRow = new HBox(10)

    private final MenuButton displayOptionsButton = new MenuButton("▾")
    private final ContextMenu displayMenu = new ContextMenu()

    RibbonSkin(Ribbon ribbon) {
        super(ribbon)

        // --- Style hooks ---
        if (!ribbon.styleClass.contains("ribbon")) {
            ribbon.styleClass.add("ribbon")
        }

        headerBox.padding = new Insets(6, 8, 6, 8)
        tabHeader.padding = new Insets(2, 0, 4, 0)

        if (!headerRow.styleClass.contains("ribbon-header-row")) {
            headerRow.styleClass.add("ribbon-header-row")
        }

        // Strip (groups area) background handled by CSS
        if (!contentPane.styleClass.contains("ribbon-strip")) {
            contentPane.styleClass.add("ribbon-strip")
        }

        displayOptionsButton.focusTraversable = false
        if (!displayOptionsButton.styleClass.contains("ribbon-display-options")) {
            displayOptionsButton.styleClass.add("ribbon-display-options")
        }
        //todo revisit this and what we need to do - managed in css at mo i think
        //displayOptionsButton.visible = false
        //displayOptionsButton.managed = false

        // --- Configure reusable header center bits (once) ---
        centerBox.alignment = Pos.BOTTOM_LEFT
        if (!centerBox.styleClass.contains("ribbon-header-center")) {
            centerBox.styleClass.add("ribbon-header-center")
        }

        if (!headerSep.styleClass.contains("ribbon-group-separator")) {
            headerSep.styleClass.add("ribbon-group-separator")
        }
        headerSep.padding = Insets.EMPTY
        HBox.setMargin(headerSep, new Insets(0, 2, 0, 2)) // tight, Office-ish gap

        if (!tabHeader.styleClass.contains("ribbon-tab-header")) {
            tabHeader.styleClass.add("ribbon-tab-header")
        }
        tabHeader.maxWidth = Double.MAX_VALUE
        HBox.setHgrow(tabHeader, Priority.ALWAYS)

        centerBox.children.setAll(headerSep, tabHeader)

        // --- Configure left box (once) ---
        leftBox.alignment = Pos.CENTER_LEFT
        if (!leftBox.styleClass.contains("ribbon-header-left")) {
            leftBox.styleClass.add("ribbon-header-left")
        }

        // --- Header: Backstage + QuickAccess + Tabs ---
        rebuildHeader(ribbon)
        ribbon.backstageProperty().addListener { _, _, _ -> rebuildHeader(ribbon) }
        ribbon.quickAccessProperty().addListener { _, _, _ -> rebuildHeader(ribbon) }

        headerBox.children.setAll(headerRow)

        // --- Strip row: groups + final separator + display options button (bottom-right) ---
        stripRow.center = groupsRow
        stripRow.setMaxWidth(Double.MAX_VALUE)

        def rhs = new HBox(6)
        rhs.alignment = Pos.BOTTOM_RIGHT
        rhs.translateY = 1 // tiny optical drop

        def finalSep = new Separator(Orientation.VERTICAL)
        if (!finalSep.styleClass.contains("ribbon-group-separator")) {
            finalSep.styleClass.add("ribbon-group-separator")
        }

        rhs.children.setAll(finalSep, displayOptionsButton)

        stripRow.right = rhs
        BorderPane.setAlignment(rhs, Pos.BOTTOM_RIGHT)
        BorderPane.setMargin(rhs, new Insets(0, 6, 4, 6))

        contentPane.children.setAll(stripRow)

        // Ensure wide fill
        contentPane.maxWidth = Double.MAX_VALUE
        groupsRow.maxWidth = Double.MAX_VALUE

        // --- Collapsed behavior ---
        ribbon.collapsedProperty().addListener { _, _, v ->
            contentPane.managed = !v
            contentPane.visible = !v
        }
        contentPane.managed = !ribbon.isCollapsed()
        contentPane.visible = !ribbon.isCollapsed()

        // --- Display options menu ---
        buildDisplayOptionsMenu(ribbon)

        // --- Tabs + selection wiring ---
        ribbon.tabs.addListener(({ ListChangeListener.Change<? extends RibbonTab> c ->
            rebuildTabs(ribbon)
        } as ListChangeListener<RibbonTab>))

        ribbon.selectedTabProperty().addListener { _, _, _ ->
            rebuildGroups(ribbon)
        }

        rebuildTabs(ribbon)
        rebuildGroups(ribbon)

        // Root
        def root = new VBox(headerBox, contentPane)
        if (!root.styleClass.contains("ribbon-root")) {
            root.styleClass.add("ribbon-root")
        }
        getChildren().add(root)
    }

    private void rebuildHeader(Ribbon ribbon) {
        // Left zone: Backstage + QuickAccess (if provided)
        leftBox.children.clear()

        def backstage = ribbon.backstage
        if (backstage != null) {
            leftBox.children.add(backstage)
        }

        def quick = ribbon.quickAccess
        if (quick != null) {
            leftBox.children.add(quick)
        }

        // Apply to header row zones
        headerRow.left = leftBox
        BorderPane.setAlignment(leftBox, Pos.CENTER_LEFT)

        headerRow.center = centerBox
        BorderPane.setAlignment(centerBox, Pos.BOTTOM_LEFT)

        // Optional: header-right controls
        headerRow.right = null
    }

    private void rebuildTabs(Ribbon ribbon) {
        tabHeader.children.clear()

        for (RibbonTab tab : ribbon.tabs) {
            ToggleButton tb = new ToggleButton(tab.text ?: "Tab")
            tb.toggleGroup = tabToggleGroup
            tb.onAction = { ribbon.setSelectedTab(tab) }

            if (!tb.styleClass.contains("ribbon-tab-button")) {
                tb.styleClass.add("ribbon-tab-button")
            }

            tabHeader.children.add(tb)

            if (ribbon.selectedTab == tab) {
                tb.selected = true
            }
        }

        // If nothing selected but tabs exist, select first
        if (ribbon.selectedTab == null && !ribbon.tabs.isEmpty()) {
            ribbon.setSelectedTab(ribbon.tabs.get(0))
            if (!tabHeader.children.isEmpty() && tabHeader.children.get(0) instanceof ToggleButton) {
                ((ToggleButton) tabHeader.children.get(0)).selected = true
            }
        }
    }

    private void rebuildGroups(Ribbon ribbon) {
        groupsRow.children.clear()

        RibbonTab tab = ribbon.selectedTab
        if (tab == null) return

        boolean first = true
        for (RibbonGroup group : tab.groups) {
            if (!first) {
                Separator sep = new Separator(Orientation.VERTICAL)
                if (!sep.styleClass.contains("ribbon-group-separator")) {
                    sep.styleClass.add("ribbon-group-separator")
                }
                groupsRow.children.add(sep)
            }
            first = false
            groupsRow.children.add(group) // group is a Control (has its own skin)
        }
    }

    private void buildDisplayOptionsMenu(Ribbon ribbon) {
        def miAutoHide = new RadioMenuItem("Auto-hide Ribbon")
        def miTabsOnly = new RadioMenuItem("Show tabs only")
        def miAlways = new RadioMenuItem("Always show Ribbon")

        def g = new ToggleGroup()
        miAutoHide.toggleGroup = g
        miTabsOnly.toggleGroup = g
        miAlways.toggleGroup = g

        switch (ribbon.displayMode) {
            case Ribbon.DisplayMode.ALWAYS_SHOW:    miAlways.selected = true; break
            case Ribbon.DisplayMode.SHOW_TABS_ONLY: miTabsOnly.selected = true; break
            case Ribbon.DisplayMode.AUTO_HIDE:      miAutoHide.selected = true; break
        }

        miAlways.onAction = {
            if (!ribbon.isCollapsible()) return
            ribbon.displayMode = Ribbon.DisplayMode.ALWAYS_SHOW
        }
        miTabsOnly.onAction = {
            if (!ribbon.isCollapsible()) return
            ribbon.displayMode = Ribbon.DisplayMode.SHOW_TABS_ONLY
        }
        miAutoHide.onAction = {
            if (!ribbon.isCollapsible()) return
            ribbon.displayMode = Ribbon.DisplayMode.AUTO_HIDE
        }

        ribbon.displayModeProperty().addListener { _, _, m ->
            switch (m) {
                case Ribbon.DisplayMode.ALWAYS_SHOW:    miAlways.selected = true; break
                case Ribbon.DisplayMode.SHOW_TABS_ONLY: miTabsOnly.selected = true; break
                case Ribbon.DisplayMode.AUTO_HIDE:      miAutoHide.selected = true; break
            }
        }

        displayMenu.items.setAll(miAutoHide, miTabsOnly, miAlways)

        displayOptionsButton.setOnAction {
            displayMenu.show(displayOptionsButton, Side.BOTTOM, 0, 0)
        }
    }
}
