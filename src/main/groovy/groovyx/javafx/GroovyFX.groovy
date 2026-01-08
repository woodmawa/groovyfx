/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package groovyx.javafx;

import groovy.lang.Closure
import groovy.transform.CompileStatic;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point helper for launching a GroovyFX application.
 *
 * <p>This class is an {@link Application} that delegates its UI construction to a
 * Groovy {@link Closure}. The closure is executed with a {@link SceneGraphBuilder}
 * as its delegate, allowing GroovyFX DSL-style UI construction.</p>
 *
 * <p>Historically GroovyFX used Swing bootstrapping; this implementation uses
 * {@link Platform#startup(Runnable)} when needed to initialize the JavaFX toolkit
 * without requiring {@code javafx.swing}.</p>
 *
 * @author jimclarke
 * @author Dierk Koenig (default delegate)
 */
@CompileStatic
public class GroovyFX extends Application {

    /**
     * The closure to execute on the JavaFX Application Thread during {@link #start(Stage)}.
     *
     * <p>This is published via {@link #start(Closure)} / {@link #start(Closure, String...)}
     * before {@link Application#launch(Class, String...)} transfers control to JavaFX.</p>
     */
    public static volatile Closure<Object> closure;

    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean(false);

    /**
     * Creates a GroovyFX application instance.
     *
     * <p>Instances are typically created by the JavaFX runtime, not directly by user code.</p>
     */
    public GroovyFX() {
        // default constructor required by JavaFX runtime
    }

    /**
     * Ensures that the JavaFX toolkit is initialized (without using Swing).
     *
     * <p>This method is safe to call multiple times. If the toolkit is already running
     * then this method is effectively a no-op.</p>
     *
     * <p>Note: This does <em>not</em> launch an {@link Application}; it only ensures the
     * toolkit is ready so that {@link Platform#runLater(Runnable)} can be used.</p>
     */
    public static void initJavaFX() {
        if (TOOLKIT_STARTED.get()) return;

        // If JavaFX is already running, Platform.runLater will succeed.
        try {
            Platform.runLater(() -> { /* already started */ });
            TOOLKIT_STARTED.set(true);
            return;
        } catch (IllegalStateException ignored) {
            // Toolkit not started yet; fall through to Platform.startup
        }

        if (!TOOLKIT_STARTED.compareAndSet(false, true)) return;

        try {
            // Platform.startup can be called exactly once per JVM.
            Platform.startup(() -> { /* no-op */ });
        } catch (IllegalStateException ignored) {
            // If someone else started it concurrently, that's fine.
        }
    }

    /**
     * JavaFX entry point invoked by the runtime after {@link Application#launch}.
     *
     * <p>Executes the user-provided {@link #closure} with a {@link SceneGraphBuilder}
     * (delegated to the {@code primaryStage}).</p>
     *
     * @param primaryStage the primary stage supplied by the JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage) {
        Closure<Object> local = closure
        if (local == null) throw new IllegalStateException("GroovyFX.start(Closure) must be called before Application launch.");

        try {
            def builder = new SceneGraphBuilder(primaryStage)
            builder.build(local)   // <- this sets DELEGATE_FIRST and rehydrates correctly
        } catch (RuntimeException re) {
            re.printStackTrace()
            throw re
        }
    }

    /**
     * Launches the GroovyFX application using the supplied builder closure.
     *
     * <p>The closure is executed on the JavaFX Application Thread and is expected to
     * build the scene graph using a {@link SceneGraphBuilder} delegate.</p>
     *
     * <p>Note: {@link Application#launch} can only be called once per JVM.</p>
     *
     * @param buildMe the code that is built in the context of a {@link SceneGraphBuilder}
     */
    public static void start(
            @DelegatesTo(value = SceneGraphBuilder, strategy = Closure.DELEGATE_FIRST)
                    Closure<Object> buildMe
    ) {
        start(buildMe, new String[0])
    }


    /**
     * Launches the GroovyFX application using the supplied builder closure and arguments.
     *
     * <p>The closure is executed on the JavaFX Application Thread and is expected to
     * build the scene graph using a {@link SceneGraphBuilder} delegate.</p>
     *
     * <p>Note: {@link Application#launch} can only be called once per JVM.</p>
     *
     * @param buildMe the code that is built in the context of a {@link SceneGraphBuilder}
     * @param args   optional application arguments passed to {@link Application#launch}
     */


    public static void start(
            @DelegatesTo(value = SceneGraphBuilder, strategy = Closure.DELEGATE_FIRST)
                    Closure<Object> buildMe,
            String... args
    ) {
        closure = buildMe
        Application.launch(GroovyFX.class, args)
    }

    /**
     * Runs the given {@link Runnable} on the JavaFX Application Thread.
     *
     * <p>If the JavaFX toolkit has not been started, this method initializes it via
     * {@link #initJavaFX()} first. The runnable is executed synchronously: this method
     * only returns after the runnable has completed.</p>
     *
     * @param r the runnable to execute on the JavaFX thread
     */
    public static void runOnFxThread(Runnable r) {
        initJavaFX();

        if (Platform.isFxApplicationThread()) {
            r.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
    }
}
