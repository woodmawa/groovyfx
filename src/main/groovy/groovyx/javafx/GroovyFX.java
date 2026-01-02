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

import groovy.lang.Closure;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * General starter application that displays a stage.
 * Historically used JFXPanel to bootstrap JavaFX; this version uses Platform.startup
 * to avoid requiring the javafx.swing module.
 *
 * @author jimclarke
 * @author Dierk Koenig added the default delegate
 */
public class GroovyFX extends Application {

    // Published via start(buildMe); accessed on FX Application Thread.
    public static volatile Closure<Object> closure;

    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean(false);

    /**
     * Initializes the JavaFX toolkit (if not already initialized) without using Swing.
     * Safe to call multiple times; subsequent calls are no-ops.
     *
     * Note: This does NOT start an Application; it only ensures the toolkit is ready.
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

    @Override
    public void start(Stage primaryStage) {
        Closure<Object> local = closure;
        if (local == null) {
            throw new IllegalStateException("GroovyFX.start(Closure) must be called before Application launch.");
        }

        try {
            local.setDelegate(new SceneGraphBuilder(primaryStage));
            InvokerHelper.invokeClosure(local, new Object[]{ this });
        } catch (RuntimeException re) {
            re.printStackTrace();
            throw re;
        }
    }

    /**
     * @param buildMe The code that is to be built in the context of a SceneGraphBuilder
     *                for the primary stage and started.
     *
     * Note: Application.launch can only be called once per JVM.
     */
    public static void start(Closure<Object> buildMe) {
        start(buildMe, new String[0]);
    }

    /**
     * Variant that allows passing Application arguments.
     */
    public static void start(Closure<Object> buildMe, String... args) {
        closure = buildMe;
        // No need to call initJavaFX(); Application.launch will initialize the toolkit.
        Application.launch(GroovyFX.class, args);
    }

    /**
     * Convenience: run something on the JavaFX thread, starting the toolkit if needed.
     * Useful for smoke tests and non-UI initialization checks.
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
