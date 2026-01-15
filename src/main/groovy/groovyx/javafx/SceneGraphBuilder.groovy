/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.javafx

import groovy.lang.DelegatesTo
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.javafx.animation.TargetHolder
import groovyx.javafx.canvas.*
import groovyx.javafx.components.Notification
import groovyx.javafx.event.GroovyCallback
import groovyx.javafx.event.GroovyEventHandler
import groovyx.javafx.factory.*
import groovyx.javafx.factory.animation.KeyFrameFactory
import groovyx.javafx.factory.animation.KeyFrameWrapper
import groovyx.javafx.factory.animation.KeyValueFactory
import groovyx.javafx.factory.animation.KeyValueSubFactory
import groovyx.javafx.factory.animation.TimelineFactory
import groovyx.javafx.module.CachedModule
import groovyx.javafx.module.UIModule
import groovyx.javafx.module.blueprint.BlueprintModule
import groovyx.javafx.module.blueprint.BlueprintRecordingBuilder
import groovyx.javafx.spi.SceneGraphAddon
import javafx.animation.*
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.chart.*
import javafx.scene.control.*
import javafx.scene.effect.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.scene.transform.*
import javafx.stage.Stage
import javafx.stage.Window
import javafx.util.Duration
import org.codehaus.groovy.runtime.MethodClosure
import groovyx.javafx.module.ModuleRegistry

import java.util.function.Consumer

import static groovyx.javafx.GroovyFXEnhancer.*

/**
 * SceneGraphBuilder
 *
 * GroovyFX DSL builder for JavaFX.
 */
@Slf4j
class SceneGraphBuilder extends FactoryBuilderSupport {
    static final String DELEGATE_PROPERTY_OBJECT_ID = "_delegateProperty:id"
    static final String DEFAULT_DELEGATE_PROPERTY_OBJECT_ID = "id"

    static final String DELEGATE_PROPERTY_OBJECT_FILL = "_delegateProperty:fill"
    static final String DEFAULT_DELEGATE_PROPERTY_OBJECT_FILL = "fill"

    static final String DELEGATE_PROPERTY_OBJECT_STROKE = "_delegateProperty:stroke"
    static final String DEFAULT_DELEGATE_PROPERTY_OBJECT_STROKE = "stroke"

    static final String CONTEXT_SCENE_KEY = "CurrentScene"
    static final String CONTEXT_DIVIDER_KEY = "CurrentDividers"

    
    private static final Random random = new Random()

    private Scene currentScene

    // Prevent re-registering factories if initialize() is called more than once
    private boolean initialized = false
    private boolean addonsLoaded = false
    private static final String DISABLE_ADDONS_PROP = "groovyfx.disableAddons"


    static {
        enhanceClasses()
    }

    SceneGraphBuilder(boolean init = true) {
        super(init)
        if ( init)
            initialize()
    }

    SceneGraphBuilder(Stage primaryStage, boolean init = true) {
        super(init)
        this.variables.primaryStage = primaryStage
        if ( init)
            initialize()
    }

    // ---- IDE-friendly explicit DSL entrypoint (works with registered PrimaryStageFactory) ----
    @CompileStatic
    Stage primaryStage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Stage) Closure cl) {
        return (Stage) invokeMethod("primaryStage", (Object) cl)
    }

    @CompileStatic
    Stage primaryStage(Map attrs,
                       @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Stage) Closure cl) {
        return (Stage) invokeMethod("primaryStage", (Object) ([attrs, cl] as Object[]))
    }

    Stage getPrimaryStage() { return (Stage) variables.primaryStage }

    Scene getCurrentScene() { return currentScene }
    void setCurrentScene(Scene scene) { this.currentScene = scene }

    SceneGraphBuilder defer(Closure c) {
        Platform.runLater { c.call() }
        return this
    }

    private void loadAddons() {
        try {
            def cl = Thread.currentThread().contextClassLoader ?: this.class.classLoader
            def loader = ServiceLoader.load(SceneGraphAddon, cl)
            for (SceneGraphAddon addon : loader) {
                try {
                    addon.apply(this)
                    log.info("Loaded SceneGraphAddon ${addon.class.name}")
                } catch (Throwable t) {
                    log.warn("Failed to apply SceneGraphAddon ${addon?.class?.name}", t)

                }
            }
        } catch (ServiceConfigurationError sce) {
            log.warn("Failed to discover SceneGraphAddon providers: ${sce.message}")
        } catch (Throwable t) {
            log.warn("Unexpected failure loading SceneGraphAddons: ${t.message}")
        }
    }

    private void loadAddonsOnce() {
        if (!initialized) {
            throw new IllegalStateException("Addons loaded before SceneGraphBuilder initialization")
        }
        if (addonsLoaded) return
        addonsLoaded = true

        if (Boolean.getBoolean(DISABLE_ADDONS_PROP)) {
            log.info("SceneGraphAddon loading disabled via -D${DISABLE_ADDONS_PROP}=true")
            return
        }
        loadAddons()
    }

    /** Manually apply an addon to this builder instance. */
    void addon(SceneGraphAddon addon) { addon.apply(this) }

    /** Manually apply an addon by class name. */
    void addon(Class<? extends SceneGraphAddon> addonClass) {
        addonClass.getDeclaredConstructor().newInstance().apply(this)
    }

    /** Manually register a factory. */
    void register(String name, Factory factory) { registerFactory(name, factory) }

    void registerIfAbsent(String name, Factory factory) {
        if (getFactories()?.containsKey(name)) {
            log.warn("Factory '${name}' already registered; skipping ${factory.class.name}")
            return
        }
        registerFactory(name, factory)
    }

    /** Subscribe to an ObservableValue (Consumer). */
    def subscribe(ObservableValue observable, Closure subscriber) {
        return observable.subscribe(subscriber as Consumer)
    }

    /** Subscribe to an ObservableValue (Runnable). */
    def subscribe(ObservableValue observable, Runnable subscriber) {
        return observable.subscribe(subscriber)
    }

    /** Show a non-blocking notification. */
    void notify(String message, Duration duration = Duration.seconds(3)) {
        Notification.show(getPrimaryStage(), message, duration)
    }

    /**
     * Show a non-blocking notification with named arguments.
     * Supports:
     *   notify(duration: 2, "Hello")
     *   notify(duration: Duration.seconds(2), "Hello")
     */
    void notify(Map args, String message) {
        def d = args?.duration
        Duration duration
        if (d instanceof Duration) {
            duration = (Duration) d
        } else if (d instanceof Number) {
            duration = Duration.seconds(((Number) d).doubleValue())
        } else {
            duration = Duration.seconds(3)
        }
        Notification.show(getPrimaryStage(), message, duration)
    }

    boolean isFxApplicationThread() { Platform.isFxApplicationThread() }

    @Override
    protected Factory resolveFactory(Object name, Map attributes, Object value) {
        // First, see if parent factory has a factory, if not, go to the builder.
        Factory factory = null
        Factory parent = getParentFactory()
        if (parent != null && parent instanceof AbstractFXBeanFactory) {
            factory = parent.resolveFactory(name, attributes, value)
        }
        if (factory) {
            // This is actually done in super.resolveFactory; do it here for child factory path.
            getProxyBuilder().getContext().put(CHILD_BUILDER, getProxyBuilder())
        } else {
            factory = super.resolveFactory(name, attributes, value)
        }
        return factory
    }

    SceneGraphBuilder submit(Object wv, Closure c) {
        if (wv == null) return this
        if (wv.class.name != "javafx.scene.web.WebView") {
            // Backward-compatible behavior: invoke immediately
            try {
                c.call(wv)
            } catch (Throwable t) {
                log.error ("submit: callback failed ", t)

            }
            return this
        }

        def submitClosure = {
            if (wv.engine.loadWorker.state == Worker.State.SUCCEEDED) {
                c.call(wv)
            } else {
                def listener = new ChangeListener<Worker.State>() {
                    @Override
                    void changed(ObservableValue<? extends Worker.State> observable,
                                 Worker.State oldState, Worker.State newState) {
                        defer {
                            switch (newState) {
                                case Worker.State.SUCCEEDED:
                                    try {
                                        c.call(wv)
                                    } catch (Throwable t) {
                                        log.error("submit: web view callback failed", t)
                                    }
                                    wv.engine.loadWorker.stateProperty().removeListener(this)
                                    break
                                case Worker.State.FAILED:
                                    log.warn(wv.engine.loadWorker.message)
                                    if (wv.engine.loadWorker.exception?.message) {
                                        log.warn(wv.engine.loadWorker.exception.message)
                                    }
                                    wv.engine.loadWorker.stateProperty().removeListener(this)
                                    break
                                case Worker.State.CANCELLED:
                                    log.warn(wv.engine.loadWorker.message)
                                    wv.engine.loadWorker.stateProperty().removeListener(this)
                                    break
                                default:
                                    break
                            }
                        }
                    }
                }
                wv.engine.loadWorker.stateProperty().addListener(listener)
            }
        }

        def safeSubmit = {
            try {
                submitClosure.call()
            } catch (Throwable t) {
                log.error("submit: web view callback failed", t)
            }
        }

        if (Platform.isFxApplicationThread()) {
            safeSubmit.call()
        } else {
            defer safeSubmit
        }
        return this
    }

    // ---- Common named constants / shortcuts ----
    private static final Map propertyMap = [
            horizontal: Orientation.HORIZONTAL,
            vertical: Orientation.VERTICAL,
            ease_both: Interpolator.EASE_BOTH,
            easeboth: Interpolator.EASE_BOTH,
            easein: Interpolator.EASE_IN,
            ease_in: Interpolator.EASE_IN,
            easeout: Interpolator.EASE_OUT,
            ease_out: Interpolator.EASE_OUT,
            discrete: Interpolator.DISCRETE,
            linear: Interpolator.LINEAR,
            indefinite: Timeline.INDEFINITE,

            top_left: Pos.TOP_LEFT,
            top_center: Pos.TOP_CENTER,
            top_right: Pos.TOP_RIGHT,
            center_left: Pos.CENTER_LEFT,
            center: Pos.CENTER,
            center_right: Pos.CENTER_RIGHT,
            bottom_left: Pos.BOTTOM_LEFT,
            bottom_center: Pos.BOTTOM_CENTER,
            bottom_right: Pos.BOTTOM_RIGHT,
            baseline_center: Pos.BASELINE_CENTER,
            baseline_right: Pos.BASELINE_RIGHT,
            baseline_left: Pos.BASELINE_LEFT,

            HORIZONTAL: Orientation.HORIZONTAL,
            VERTICAL: Orientation.VERTICAL,
            EASEBOTH: Interpolator.EASE_BOTH,
            EASE_BOTH: Interpolator.EASE_BOTH,
            EASEIN: Interpolator.EASE_IN,
            EASE_IN: Interpolator.EASE_IN,
            EASEOUT: Interpolator.EASE_OUT,
            EASE_OUT: Interpolator.EASE_OUT,
            DISCRETE: Interpolator.DISCRETE,
            LINEAR: Interpolator.LINEAR,
            INDEFINITE: Timeline.INDEFINITE,
            TOP: VPos.TOP,
            BOTTOM: VPos.BOTTOM,
            LEFT: HPos.LEFT,
            RIGHT: HPos.RIGHT,
            TOP_LEFT: Pos.TOP_LEFT,
            TOP_CENTER: Pos.TOP_CENTER,
            TOP_RIGHT: Pos.TOP_RIGHT,
            CENTER_LEFT: Pos.CENTER_LEFT,
            CENTER: Pos.CENTER,
            CENTER_RIGHT: Pos.CENTER_RIGHT,
            BOTTOM_LEFT: Pos.BOTTOM_LEFT,
            BOTTOM_CENTER: Pos.BOTTOM_CENTER,
            BOTTOM_RIGHT: Pos.BOTTOM_RIGHT,
            BASELINE_CENTER: Pos.BASELINE_CENTER,
            BASELINE_RIGHT: Pos.BASELINE_RIGHT,
            BASELINE_LEFT: Pos.BASELINE_LEFT
    ]

    def propertyMissing(String name) {
        // 1) Hex colors like "#336699"
        if (name?.startsWith("#")) {
            return Color.web(name)
        }

        // 2) Builder variables
        if (variables != null && variables.containsKey(name)) {
            return variables[name]
        }

        // 3) CSS color names
        try {
            def c = Color.web(name)
            setVariable(name, c)
            setVariable(name.toUpperCase(), c)
            return c
        } catch (Throwable ignored) {
            // fall through
        }

        throw new MissingPropertyException("Unrecognized property: ${name}", name, this.class)
    }

    Color rgb(int r, int g, int b) { Color.rgb(r, g, b) }
    Color rgb(int r, int g, int b, float alpha) { Color.rgb(r, g, b, alpha) }
    Color rgba(int r, int g, int b, float alpha) { rgb(r, g, b, alpha) }
    Color hsb(int hue, float saturation, float brightness, float alpha) { Color.hsb(hue, saturation, brightness, alpha) }
    Color hsb(int hue, float saturation, float brightness) { Color.hsb(hue, saturation, brightness) }

    // ---- Bean factory routing ----
    @Override
    void registerBeanFactory(String nodeName, String groupName, Class beanClass) {
        // Special handling based on type
        if (ContextMenu.isAssignableFrom(beanClass)) {
            // Ensure ContextMenu uses the dedicated factory (prevents misrouting to MenuFactory)
            registerFactory nodeName, groupName, new ContextMenuFactory()
        } else if (MenuBar.isAssignableFrom(beanClass) ||
                MenuButton.isAssignableFrom(beanClass) ||
                SplitMenuButton.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new MenuFactory(beanClass)
        } else if (MenuItem.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new MenuItemFactory(beanClass)
        } else if (TreeItem.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new TreeItemFactory(beanClass)
        } else if (TableView.isAssignableFrom(beanClass) || TableColumn.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new TableFactory(beanClass)
        } else if (Labeled.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new LabeledFactory(beanClass)
        } else if (Control.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new ControlFactory(beanClass)
        } else if (Scene.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new SceneFactory(beanClass)
        } else if (Tab.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new TabFactory(beanClass)
        } else if (Text.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new TextFactory(beanClass)
        } else if (Shape.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new ShapeFactory(beanClass)
        } else if (Transform.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new TransformFactory(beanClass)
        } else if (Effect.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new EffectFactory(beanClass)
        } else if (Parent.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new ContainerFactory(beanClass)
        } else if (Window.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new StageFactory(beanClass)
        } else if (XYChart.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new XYChartFactory(beanClass)
        } else if (PieChart.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new PieChartFactory(beanClass)
        } else if (Axis.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new AxisFactory(beanClass)
        } else if (XYChart.Series.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new XYSeriesFactory(beanClass)
        } else if (CanvasOperation.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new CanvasOperationFactory(beanClass)
        } else if (Node.isAssignableFrom(beanClass)) {
            registerFactory nodeName, groupName, new NodeFactory(beanClass)
        } else {
            super.registerBeanFactory(nodeName, groupName, beanClass)
        }
    }

    // ---- Helper: safe registration (doesn't crash builder init if optional class missing) ----
    private void safeRegisterFactory(String name, Factory factory) {
        try {
            registerFactory(name, factory)
        } catch (Throwable t) {
            log.warn("Skipping factory '${name}' (${factory?.class?.name}): ${t.message}")
        }
    }

    void registerStages() {
        registerFactory "stage", new StageFactory(Stage)
        registerFactory "primaryStage", new PrimaryStageFactory(Stage)
        registerFactory "popup", new PopupFactory()

        // Choosers are not stages; register if available in this build.
        safeRegisterFactory("fileChooser", new FileChooserFactory())
        safeRegisterFactory("directoryChooser", new DirectoryChooserFactory())

        registerFactory "filter", new FilterFactory()

        registerFactory "onHidden", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onHiding", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onShowing", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onShown", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onCloseRequest", new ClosureHandlerFactory(GroovyEventHandler)
    }

    // register this one first
    void registerNodes() {
        registerFactory "bean", new CustomNodeFactory(Object)
        registerFactory "node", new CustomNodeFactory(Node)
        registerFactory "nodes", new CustomNodeFactory(List, true)
        registerFactory "container", new CustomNodeFactory(Parent)

        registerFactory "imageView", new ImageViewFactory(ImageView)
        registerFactory "image", new ImageFactory(Image)
        registerFactory "clip", new ClipFactory(ClipHolder)
        registerFactory "fxml", new FXMLFactory()

        registerFactory "fxaction", new ActionFactory()
        registerFactory "actions", new CollectionFactory()
        registerFactory "noparent", new CollectionFactory()
    }

    void registerContainers() {
        registerFactory "scene", new SceneFactory(Scene)
        registerFactory "stylesheets", new StylesheetFactory(List)
        registerFactory "stylesheet", new StylesheetFactory(List)   // alias
        registerFactory "resource", new ResourceFactory()

        registerFactory "pane", new ContainerFactory(Pane)
        registerFactory "region", new ContainerFactory(Region)
        registerFactory "anchorPane", new ContainerFactory(AnchorPane)
        registerFactory "borderPane", new ContainerFactory(BorderPane)
        registerFactory "flowPane", new ContainerFactory(FlowPane)
        registerFactory "hbox", new ContainerFactory(HBox)
        registerFactory "vbox", new ContainerFactory(VBox)
        registerFactory "stackPane", new ContainerFactory(StackPane)
        registerFactory "tilePane", new ContainerFactory(TilePane)
        registerFactory "group", new ContainerFactory(Group)
        registerFactory "gridPane", new ContainerFactory(GridPane)
        registerFactory "textFlow", new ContainerFactory(TextFlow)

        registerFactory "constraint", new GridConstraintFactory(GridConstraint)
        registerFactory "rowConstraints", new GridConstraintFactory(RowConstraints)
        registerFactory "columnConstraints", new GridConstraintFactory(ColumnConstraints)

        registerFactory "row", new GridRowColumnFactory(GridRow)
        registerFactory "column", new GridRowColumnFactory(GridColumn)

        registerFactory "top", new BorderPanePositionFactory("top")
        registerFactory "bottom", new BorderPanePositionFactory("bottom")
        registerFactory "left", new BorderPanePositionFactory("left")
        registerFactory "right", new BorderPanePositionFactory("right")
        registerFactory "center", new BorderPanePositionFactory("center")
    }

    void registerCanvas() {
        CanvasFactory cf = new CanvasFactory()
        registerFactory "canvas", cf

        cf.registerFactory "appendSVGPath", new CanvasOperationFactory(AppendSVGPathOperation)
        cf.registerFactory "applyEffect", new CanvasOperationFactory(ApplyEffectOperation)
        cf.registerFactory "arc", new CanvasOperationFactory(ArcOperation)
        cf.registerFactory "arcTo", new CanvasOperationFactory(ArcToOperation)
        cf.registerFactory "path", new CanvasOperationFactory(BeginPathOperation)
        cf.registerFactory "bezierCurveTo", new CanvasOperationFactory(BezierCurveToOperation)
        cf.registerFactory "clearRect", new CanvasOperationFactory(ClearRectOperation)
        cf.registerFactory "clip", new CanvasOperationFactory(ClipOperation)
        cf.registerFactory "closePath", new CanvasOperationFactory(ClosePathOperation)
        cf.registerFactory "drawImage", new CanvasOperationFactory(DrawImageOperation)
        cf.registerFactory "effect", new CanvasOperationFactory(SetEffectOperation)
        cf.registerFactory "fillPath", new CanvasOperationFactory(FillOperation)
        cf.registerFactory "fill", new CanvasOperationFactory(SetFillOperation)
        cf.registerFactory "fillArc", new CanvasOperationFactory(FillArcOperation)
        cf.registerFactory "fillOval", new CanvasOperationFactory(FillOvalOperation)
        cf.registerFactory "fillPolygon", new CanvasOperationFactory(FillPolygonOperation)
        cf.registerFactory "fillRect", new CanvasOperationFactory(FillRectOperation)
        cf.registerFactory "fillRoundRect", new CanvasOperationFactory(FillRoundRectOperation)
        cf.registerFactory "fillRule", new CanvasOperationFactory(SetFillRuleOperation)
        cf.registerFactory "fillText", new CanvasOperationFactory(FillTextOperation)
        cf.registerFactory "font", new CanvasOperationFactory(SetFontOperation)
        cf.registerFactory "globalAlpha", new CanvasOperationFactory(SetGlobalAlphaOperation)
        cf.registerFactory "globalBlendMode", new CanvasOperationFactory(SetGlobalBlendModeOperation)
        cf.registerFactory "lineCap", new CanvasOperationFactory(SetLineCapOperation)
        cf.registerFactory "lineJoin", new CanvasOperationFactory(SetLineJoinOperation)
        cf.registerFactory "lineTo", new CanvasOperationFactory(LineToOperation)
        cf.registerFactory "lineWidth", new CanvasOperationFactory(SetLineWidthOperation)
        cf.registerFactory "miterLimit", new CanvasOperationFactory(SetMiterLimitOperation)
        cf.registerFactory "moveTo", new CanvasOperationFactory(MoveToOperation)
        cf.registerFactory "quadraticCurveTo", new CanvasOperationFactory(QuadraticCurveToOperation)
        cf.registerFactory "rect", new CanvasOperationFactory(RectOperation)
        cf.registerFactory "restore", new CanvasOperationFactory(RestoreOperation)
        cf.registerFactory "rotate", new CanvasOperationFactory(RotateOperation)
        cf.registerFactory "save", new CanvasOperationFactory(SaveOperation)
        cf.registerFactory "scale", new CanvasOperationFactory(ScaleOperation)
        cf.registerFactory "stroke", new CanvasOperationFactory(SetStrokeOperation)
        cf.registerFactory "strokePath", new CanvasOperationFactory(StrokeOperation)
        cf.registerFactory "strokeArc", new CanvasOperationFactory(StrokeArcOperation)
        cf.registerFactory "strokeLine", new CanvasOperationFactory(StrokeLineOperation)
        cf.registerFactory "strokeOval", new CanvasOperationFactory(StrokeOvalOperation)
        cf.registerFactory "strokePolygon", new CanvasOperationFactory(StrokePolygonOperation)
        cf.registerFactory "strokePolyline", new CanvasOperationFactory(StrokePolylineOperation)
        cf.registerFactory "strokeRect", new CanvasOperationFactory(StrokeRectOperation)
        cf.registerFactory "strokeRoundRect", new CanvasOperationFactory(StrokeRoundRectOperation)
        cf.registerFactory "strokeText", new CanvasOperationFactory(StrokeTextOperation)
        cf.registerFactory "textAlign", new CanvasOperationFactory(SetTextAlignOperation)
        cf.registerFactory "textBaseline", new CanvasOperationFactory(SetTextBaselineOperation)
        cf.registerFactory "setTransform", new CanvasOperationFactory(SetTransformOperation)
        cf.registerFactory "transform", new CanvasOperationFactory(TransformOperation)
        cf.registerFactory "translate", new CanvasOperationFactory(TranslateOperation)
        cf.registerFactory "operation", new CanvasClosureOperationFactory(ClosureOperation)

        DrawFactory df = new DrawFactory()
        registerFactory "draw", df
        df.childFactories = cf.childFactories
    }

    void registerBinding() {
        BindFactory bf = new BindFactory()
        registerFactory "bind", bf

        registerFactory "onChange", new ChangeFactory(ChangeListener)
        registerFactory "onInvalidate", new ChangeFactory(InvalidationListener)
    }

    void registerThreading() {
        registerExplicitMethod "defer", this.&defer

        // IMPORTANT:
        // MethodClosure resolution can be ambiguous with overloads.
        // Use a dispatcher closure so "notify" works for both call styles:
        //   notify("msg")
        //   notify("msg", Duration.seconds(2))
        //   notify(duration: 2, "msg")
        registerExplicitMethod "notify", { Object... a ->
            if (a == null || a.length == 0) return null
            if (a.length == 1) {
                def x = a[0]
                if (x instanceof Map) return null // invalid form alone
                return notify(String.valueOf(x))
            }
            if (a.length == 2) {
                def first = a[0]
                def second = a[1]
                if (first instanceof Map) {
                    return notify((Map) first, String.valueOf(second))
                }
                if (second instanceof Duration) {
                    return notify(String.valueOf(first), (Duration) second)
                }
                if (second instanceof Number) {
                    return notify(String.valueOf(first), Duration.seconds(((Number) second).doubleValue()))
                }
                // fallback
                return notify(String.valueOf(first))
            }
            // Ignore extras
            return notify(String.valueOf(a[0]))
        }
    }

    void registerMenus() {
        registerFactory "menuBar", new MenuFactory(MenuBar)
        registerFactory "menuButton", new MenuFactory(MenuButton)
        registerFactory "splitMenuButton", new MenuFactory(SplitMenuButton)

        registerFactory "menu", new MenuItemFactory(Menu)
        registerFactory "menuItem", new MenuItemFactory(MenuItem)
        registerFactory "checkMenuItem", new MenuItemFactory(CheckMenuItem)
        registerFactory "customMenuItem", new MenuItemFactory(CustomMenuItem)
        registerFactory "separatorMenuItem", new MenuItemFactory(SeparatorMenuItem)
        registerFactory "radioMenuItem", new MenuItemFactory(RadioMenuItem)
    }

    //new ribbon menu
    void registerRibbon() {
        registerFactory "ribbon", new RibbonFactory()
        registerFactory "ribbonTab", new RibbonTabFactory()
        registerFactory "ribbonGroup", new RibbonGroupFactory()

        // optional niceties / aliases
        registerFactory "quickAccess", new RibbonQuickAccessFactory()
        registerFactory "backstage", new RibbonBackstageFactory()   // if you do "File" panel later
    }

    void registerCharts() {
        registerFactory "pieChart", new PieChartFactory(PieChart)
        registerFactory "lineChart", new XYChartFactory(LineChart)
        registerFactory "areaChart", new XYChartFactory(AreaChart)

        registerFactory "stackedAreaChart", new XYChartFactory(StackedAreaChart)
        registerFactory "bubbleChart", new XYChartFactory(BubbleChart)
        registerFactory "barChart", new XYChartFactory(BarChart)
        registerFactory "stackedBarChart", new XYChartFactory(StackedBarChart)
        registerFactory "scatterChart", new XYChartFactory(ScatterChart)

        registerFactory "numberAxis", new AxisFactory(NumberAxis)
        registerFactory "categoryAxis", new AxisFactory(CategoryAxis)
        registerFactory "series", new XYSeriesFactory(XYChart.Series)
    }


    void registerTransforms() {
        registerFactory "affine", new TransformFactory(Affine)
        registerFactory "rotate", new TransformFactory(Rotate)
        registerFactory "scale", new TransformFactory(Scale)
        registerFactory "shear", new TransformFactory(Shear)
        registerFactory "translate", new TransformFactory(Translate)
    }

    void registerShapes() {
        registerFactory "arc", new ShapeFactory(Arc)
        registerFactory "circle", new ShapeFactory(Circle)
        registerFactory "cubicCurve", new ShapeFactory(CubicCurve)
        registerFactory "ellipse", new ShapeFactory(Ellipse)
        registerFactory "line", new ShapeFactory(Line)
        registerFactory "polygon", new ShapeFactory(Polygon)
        registerFactory "polyline", new ShapeFactory(Polyline)
        registerFactory "quadCurve", new ShapeFactory(QuadCurve)
        registerFactory "rectangle", new ShapeFactory(Rectangle)
        registerFactory "svgPath", new ShapeFactory(SVGPath)

        PathFactory pathFactory = new PathFactory(Path)
        registerFactory "path", pathFactory

        pathFactory.registerFactory "arcTo", new PathElementFactory(ArcTo)
        pathFactory.registerFactory "closePath", new PathElementFactory(ClosePath)
        pathFactory.registerFactory "cubicCurveTo", new PathElementFactory(CubicCurveTo)
        pathFactory.registerFactory "hLineTo", new PathElementFactory(HLineTo)
        pathFactory.registerFactory "lineTo", new PathElementFactory(LineTo)
        pathFactory.registerFactory "moveTo", new PathElementFactory(MoveTo)
        pathFactory.registerFactory "quadCurveTo", new PathElementFactory(QuadCurveTo)
        pathFactory.registerFactory "vLineTo", new PathElementFactory(VLineTo)

        registerFactory "text", new TextFactory(Text)

        registerFactory "linearGradient", new LinearGradientFactory()
        registerFactory "radialGradient", new RadialGradientFactory()
        registerFactory "stop", new StopFactory()
        registerFactory "fill", new FillFactory()
        registerFactory "stroke", new StrokeFactory()
    }

    void registerControls() {
        // labeled
        registerFactory "button", new LabeledFactory(Button)
        registerFactory "checkBox", new LabeledFactory(CheckBox)
        registerFactory "label", new LabeledFactory(Label)
        registerFactory "choiceBox", new LabeledFactory(ChoiceBox)
        registerFactory "hyperlink", new LabeledFactory(Hyperlink)
        registerFactory "tooltip", new LabeledFactory(Tooltip)
        registerFactory "radioButton", new LabeledFactory(RadioButton)
        registerFactory "toggleButton", new LabeledFactory(ToggleButton)

        registerFactory "onSelect", new CellFactory()
        registerFactory "cellFactory", new CellFactory()
        registerFactory "onAction", new ClosureHandlerFactory(GroovyEventHandler)

        // regular controls
        registerFactory "scrollBar", new ControlFactory(ScrollBar)
        registerFactory "slider", new ControlFactory(Slider)
        registerFactory "separator", new ControlFactory(Separator)
        registerFactory "listView", new ListViewFactory()
        registerFactory "textArea", new ControlFactory(TextArea)
        registerFactory "textField", new ControlFactory(TextField)
        registerFactory "passwordField", new ControlFactory(PasswordField)
        registerFactory "progressBar", new ControlFactory(ProgressBar)
        registerFactory "progressIndicator", new ControlFactory(ProgressIndicator)
        registerFactory "scrollPane", new ScrollPaneFactory()
        registerFactory "comboBox", new ControlFactory(ComboBox)

        // context menus (node + attachment handled in ControlFactory)
        registerFactory "contextMenu", new ContextMenuFactory()

        registerFactory "accordion", new AccordionFactory()
        registerFactory "splitPane", new SplitPaneFactory()
        registerFactory "tabPane", new TabPaneFactory()
        registerFactory "titledPane", new ControlFactory(TitledPane)
        registerFactory "dividerPosition", new DividerPositionFactory(DividerPosition)
        registerFactory "tab", new TabFactory(Tab)
        registerFactory "toolBar", new ToolBarFactory()
        registerFactory "buttonBar", new ButtonBarFactory()
        registerFactory "colorPicker", new ControlFactory(ColorPicker)
        registerFactory "pagination", new ControlFactory(Pagination)
        registerFactory "datePicker", new ControlFactory(DatePicker)
        registerFactory "spinner", new ControlFactory(Spinner)

        registerFactory "treeView", new ControlFactory(TreeView)
        registerFactory "treeItem", new TreeItemFactory(TreeItem)

        registerFactory "tableView", new TableFactory(TableView)
        registerFactory "tableColumn", new TableFactory(TableColumn)

        registerFactory "title", new TitledFactory(TitledNode)
        registerFactory "content", new TitledFactory(TitledContent)

        registerFactory "graphic", new GraphicFactory(Graphic)

        // tree events
        TreeItemFactory.treeItemEvents.each { String name, eventType ->
            registerFactory name, new ClosureHandlerFactory(GroovyEventHandler)
        }

        registerFactory "onEditCancel", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onEditCommit", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onEditStart", new ClosureHandlerFactory(GroovyEventHandler)
    }

    /** Register a node name that maps to a class with a static build(builder, attrs, body) method OR a no-arg ctor. */
    void registerComponentNode(String name, Class componentClass) {
        registerFactory(name, new ComponentClassFactory(componentClass))
    }

    /** @deprecated use registerComponentNode */
    @Deprecated
    void registerComponent(String name, Class componentClass) {
        registerComponentNode(name, componentClass)
    }

    void registerComponentWidgets() {
        // Modern Components / Widgets
        CardFactory cardFactory = new CardFactory()
        registerFactory "card", cardFactory
        cardFactory.registerFactory "cardHeader", new CardSectionFactory("cardHeader")
        cardFactory.registerFactory "cardBody", new CardSectionFactory("cardBody")
        cardFactory.registerFactory "cardFooter", new CardSectionFactory("cardFooter")

        registerFactory "badge", new BadgeFactory()
        registerFactory "icon", new IconFactory()
        registerFactory "toggleSwitch", new ToggleSwitchFactory()
        registerFactory "field", new FormFieldFactory()
        registerFactory "formLayout", new FormLayoutFactory()
        registerFactory "responsivePane", new ResponsivePaneFactory()
        registerFactory "carousel", new CarouselFactory()
        registerFactory "slide", new CarouselSlideFactory()
        registerFactory "node", new ExistingNodeFactory()

        //registerFactory "notification", new NotificationFactory()   // if you have one
    }

    void registerEffects() {
        // Dummy node for attaching child effects
        registerFactory "effect", new EffectFactory(Effect)

        registerFactory "blend", new EffectFactory(Blend)
        registerFactory "bloom", new EffectFactory(Bloom)
        registerFactory "boxBlur", new EffectFactory(BoxBlur)
        registerFactory "colorAdjust", new EffectFactory(ColorAdjust)
        registerFactory "colorInput", new EffectFactory(ColorInput)
        registerFactory "displacementMap", new EffectFactory(DisplacementMap)
        registerFactory "dropShadow", new EffectFactory(DropShadow)
        registerFactory "gaussianBlur", new EffectFactory(GaussianBlur)
        registerFactory "glow", new EffectFactory(Glow)
        registerFactory "imageInput", new EffectFactory(ImageInput)
        registerFactory "innerShadow", new EffectFactory(InnerShadow)
        registerFactory "lighting", new EffectFactory(Lighting)
        registerFactory "motionBlur", new EffectFactory(MotionBlur)
        registerFactory "perspectiveTransform", new EffectFactory(PerspectiveTransform)
        registerFactory "reflection", new EffectFactory(Reflection)
        registerFactory "sepiaTone", new EffectFactory(SepiaTone)
        registerFactory "shadow", new EffectFactory(Shadow)

        registerFactory "topInput", new EffectFactory(EffectWrapper)
        registerFactory "bottomInput", new EffectFactory(EffectWrapper)
        registerFactory "bumpInput", new EffectFactory(EffectWrapper)
        registerFactory "contentInput", new EffectFactory(EffectWrapper)
        registerFactory "distant", new EffectFactory(Light.Distant)
        registerFactory "point", new EffectFactory(Light.Point)
        registerFactory "spot", new EffectFactory(Light.Spot)
    }

    void registerEventHandlers() {
        ClosureHandlerFactory eh = new ClosureHandlerFactory(GroovyEventHandler)
        for (property in AbstractNodeFactory.nodeEvents) {
            registerFactory property, eh
        }
    }

    private static Class tryLoadClass(String fqcn) {
        try {
            def cl = Thread.currentThread().contextClassLoader ?: SceneGraphBuilder.class.classLoader
            return (Class) Class.forName(fqcn, false, cl)
        } catch (Throwable ignored) {
            return null
        }
    }

    private static boolean isInstanceOf(Object obj, String fqcn) {
        if (obj == null) return false
        Class<?> c = obj.getClass()
        while (c != null) {
            if (c.getName() == fqcn) return true
            c = c.getSuperclass()
        }
        return false
    }


    void registerWeb() {
        Class<?> webViewClass = tryLoadClass("javafx.scene.web.WebView")
        Class<?> htmlEditorClass = tryLoadClass("javafx.scene.web.HTMLEditor")
        if (webViewClass == null || htmlEditorClass == null) {
            log.info("javafx-web not present; skipping WebView/HTMLEditor DSL registration")
            return
        }

        registerFactory "webView", new WebFactory(webViewClass)
        registerFactory "htmlEditor", new WebFactory(htmlEditorClass)

        registerFactory "onLoad", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onError", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onAlert", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onResized", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "onVisibilityChanged", new ClosureHandlerFactory(GroovyEventHandler)
        registerFactory "createPopupHandler", new ClosureHandlerFactory(GroovyCallback)
        registerFactory "confirmHandler", new ClosureHandlerFactory(GroovyCallback)
        registerFactory "promptHandler", new ClosureHandlerFactory(GroovyCallback)
    }

    void registerTransition() {
        registerFactory "fadeTransition", new TransitionFactory(FadeTransition)
        registerFactory "fillTransition", new TransitionFactory(FillTransition)
        registerFactory "parallelTransition", new TransitionFactory(ParallelTransition)
        registerFactory "pauseTransition", new TransitionFactory(PauseTransition)
        registerFactory "rotateTransition", new TransitionFactory(RotateTransition)
        registerFactory "scaleTransition", new TransitionFactory(ScaleTransition)
        registerFactory "translateTransition", new TransitionFactory(TranslateTransition)
        registerFactory "sequentialTransition", new TransitionFactory(SequentialTransition)
        registerFactory "pathTransition", new TransitionFactory(PathTransition)
        registerFactory "strokeTransition", new TransitionFactory(StrokeTransition)
        registerFactory "transition", new TransitionFactory(Transition)

        TimelineFactory tf = new TimelineFactory(Timeline)
        registerFactory "timeline", tf

        KeyFrameFactory kf = new KeyFrameFactory(KeyFrameWrapper)
        tf.registerFactory "at", kf

        KeyValueFactory kvf = new KeyValueFactory(TargetHolder)
        kf.registerFactory "change", kvf
        kvf.registerFactory "to", new KeyValueSubFactory(Object)
        kvf.registerFactory "tween", new KeyValueSubFactory(Interpolator)

        registerFactory "onFinished", new ClosureHandlerFactory(GroovyEventHandler)
    }

    void registerMedia() {
        Class<?> mediaViewClass = tryLoadClass("javafx.scene.media.MediaView")
        Class<?> mediaPlayerClass = tryLoadClass("javafx.scene.media.MediaPlayer")
        if (mediaViewClass == null || mediaPlayerClass == null) {
            log.info("javafx-media not present; skipping MediaView/MediaPlayer DSL registration")
            return
        }

        Class mv = tryLoadClass("javafx.scene.media.MediaView")
        Class mp = tryLoadClass("javafx.scene.media.MediaPlayer")
        if (mv == null || mp == null) return

        registerFactory "mediaView", new MediaViewFactory(mv)
        registerFactory "mediaPlayer", new MediaPlayerFactory(mp)
    }

    /**
     * Compatibility API.
     * Run this closure in the builder.
     */
    Object build(@DelegatesTo(value = SceneGraphBuilder, strategy = Closure.DELEGATE_FIRST) Closure c) {
        c = c.rehydrate(this, c.owner, c.thisObject)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        return c.call()
    }

    private static final Closure postCompletionDelegate = { FactoryBuilderSupport builder, Object parent, Object node ->
        if (isInstanceOf(parent, "javafx.scene.media.MediaView") && isInstanceOf(node, "javafx.scene.media.MediaPlayer")) {
            parent.mediaPlayer = node
        } else if (parent instanceof Stage && node instanceof Scene) {
            parent.scene = node
        } else if (node instanceof FXMLLoaderBuilder) {
            node = node.build()
        }
    }

    // Strongly typed + removes id from attrs so it doesn't leak into bean properties.
    private static final Closure idDelegate = { FactoryBuilderSupport builder, node, Map attributes ->
        def id = attributes?.remove("id")
        if (id != null) {
            builder.setVariable(id.toString(), node)
        }
    }

    private void initialize() {

        if (initialized) return
        initialized = true


        this[DELEGATE_PROPERTY_OBJECT_ID] = DEFAULT_DELEGATE_PROPERTY_OBJECT_ID
        this[DELEGATE_PROPERTY_OBJECT_FILL] = DEFAULT_DELEGATE_PROPERTY_OBJECT_FILL
        this[DELEGATE_PROPERTY_OBJECT_STROKE] = DEFAULT_DELEGATE_PROPERTY_OBJECT_STROKE

        addPostNodeCompletionDelegate(postCompletionDelegate)
        addAttributeDelegate(NodeFactory.attributeDelegate)
        addAttributeDelegate(idDelegate)

        // register DSL factories (PER INSTANCE)
        def registrations = [
                this.&registerStages,
                this.&registerNodes,
                this.&registerContainers,
                this.&registerComponentWidgets,     //register new widgets and components added during rebuild
                this.&registerRibbon,               // add new ribbon menus
                this.&registerShapes,
                this.&registerTransforms,
                this.&registerEffects,
                this.&registerCharts,
                this.&registerControls,
                this.&registerMenus,
                this.&registerMedia,
                this.&registerWeb,
                this.&registerEventHandlers,
                this.&registerBinding,
                this.&registerThreading,
                this.&registerCanvas,
                this.&registerTransition
        ]

        registrations.each { Closure c -> c.call() }

        // Public API: define any special/legacy colors as variables
        setVariable("groovyblue", Color.rgb(99, 152, 170))
        setVariable("GROOVYBLUE", Color.rgb(99, 152, 170))

        // Optional: common CSS color names
        def commonColorNames = [
                "black", "white", "red", "green", "blue", "yellow", "cyan", "magenta",
                "gray", "grey", "lightgray", "darkgray", "orange", "pink", "purple", "brown",
                "transparent"
        ]
        commonColorNames.each { n ->
            try {
                def c = Color.web(n)
                setVariable(n, c)
                setVariable(n.toUpperCase(), c)
            } catch (ignored) {
                // ignore if not recognized in this JavaFX version
            }
        }

        propertyMap.each { name, value -> setVariable(name, value) }

        // Discover external component libraries (SPI)
        loadAddonsOnce()
    }

    /**
     * new modular javafx capability
     *
     */

    UIModule compile(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SceneGraphBuilder) Closure<?> dsl) {
        new CachedModule(dsl)
    }

    UIModule module(
            String name,
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SceneGraphBuilder) Closure<?> dsl
    ) {
        def m = compile(dsl)
        ModuleRegistry.register(name, m)
        return m
    }

    static BlueprintModule blueprint(@DelegatesTo(value = BlueprintRecordingBuilder, strategy = Closure.DELEGATE_FIRST) Closure cl) {
        // v1: keep it small + explicit, expand as needed
        Map<String, Class<? extends Node>> typeIndex = defaultBlueprintTypeIndex()
        def b = new BlueprintRecordingBuilder(typeIndex)
        def bp = b.build(cl)
        return new BlueprintModule(blueprint: bp)
    }

    private static Map<String, Class<? extends Node>> defaultBlueprintTypeIndex() {
        [
                // Layout
                pane        : Pane,
                region      : Region,
                group       : Group,

                vbox        : VBox,
                hbox        : HBox,
                stackPane  : StackPane,
                borderPane : BorderPane,
                gridPane   : GridPane,
                flowPane   : FlowPane,
                tilePane   : TilePane,

                // Controls
                label       : Label,
                button      : Button,
                textField  : TextField,
                textArea   : TextArea,
                checkBox   : CheckBox,
                comboBox   : ComboBox,

                listView   : ListView,
                tableView  : TableView,
                treeView   : TreeView,

                scrollPane : ScrollPane,
                splitPane  : SplitPane,
                tabPane    : TabPane,
                toolBar    : ToolBar,

                // Misc / common
                separator  : Separator,
                progressBar: ProgressBar
        ] as Map<String, Class<? extends Node>>
    }

}
