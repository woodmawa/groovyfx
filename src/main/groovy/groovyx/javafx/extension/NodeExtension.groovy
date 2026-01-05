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
package groovyx.javafx.extension

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.util.Subscription
import javafx.beans.value.ObservableValue
import java.util.function.Consumer

/**
 * @author Andres Almiray
 */
class NodeExtension {
    static void onDragDetected(Node self, Closure listener) { self.setOnDragDetected(listener as EventHandler) }

    static void onDragDone(Node self, Closure listener) { self.setOnDragDone(listener as EventHandler) }

    static void onDragDropped(Node self, Closure listener) { self.setOnDragDropped(listener as EventHandler) }

    static void onDragEntered(Node self, Closure listener) { self.setOnDragEntered(listener as EventHandler) }

    static void onDragExited(Node self, Closure listener) { self.setOnDragExited(listener as EventHandler) }

    static void onDragOver(Node self, Closure listener) { self.setOnDragOver(listener as EventHandler) }

    static void onInputMethodTextChanged(Node self, Closure listener) {
        self.setOnInputMethodTextChanged(listener as EventHandler)
    }

    static void onKeyPressed(Node self, Closure listener) { self.setOnKeyPressed(listener as EventHandler) }

    static void onKeyReleased(Node self, Closure listener) { self.setOnKeyReleased(listener as EventHandler) }

    static void onKeyTyped(Node self, Closure listener) { self.setOnKeyTyped(listener as EventHandler) }

    static void onMouseDragEntered(Node self, Closure listener) { self.setOnMouseDragEntered(listener as EventHandler) }

    static void onMouseClicked(Node self, Closure listener) { self.setOnMouseClicked(listener as EventHandler) }

    static void onMouseDragExited(Node self, Closure listener) { self.setOnMouseDragExited(listener as EventHandler) }

    static void onMouseDragged(Node self, Closure listener) { self.setOnMouseDragged(listener as EventHandler) }

    static void onMouseDragOver(Node self, Closure listener) { self.setOnMouseDragOver(listener as EventHandler) }

    static void onMouseDragReleased(Node self, Closure listener) {
        self.setOnMouseDragReleased(listener as EventHandler)
    }

    static void onMouseEntered(Node self, Closure listener) { self.setOnMouseEntered(listener as EventHandler) }

    static void onMouseExited(Node self, Closure listener) { self.setOnMouseExited(listener as EventHandler) }

    static void onMouseMoved(Node self, Closure listener) { self.setOnMouseMoved(listener as EventHandler) }

    static void onMousePressed(Node self, Closure listener) { self.setOnMousePressed(listener as EventHandler) }

    static void onMouseReleased(Node self, Closure listener) { self.setOnMouseReleased(listener as EventHandler) }

    static void onScroll(Node self, Closure listener) { self.setOnScroll(listener as EventHandler) }

    static void onScrollStarted(Node self, Closure listener) { self.setOnScrollStarted(listener as EventHandler) }

    static void onScrollFinished(Node self, Closure listener) { self.setOnScrollFinished(listener as EventHandler) }

    static void onRotationStarted(Node self, Closure listener) { self.setOnRotationStarted(listener as EventHandler) }

    static void onRotationFinished(Node self, Closure listener) { self.setOnRotationFinished(listener as EventHandler) }

    static void onSwipeLeft(Node self, Closure listener) { self.setOnSwipeLeft(listener as EventHandler) }

    static void onSwipeRight(Node self, Closure listener) { self.setOnSwipeRight(listener as EventHandler) }

    static void onSwipeUp(Node self, Closure listener) { self.setOnSwipeUp(listener as EventHandler) }

    static void onSwipeDown(Node self, Closure listener) { self.setOnSwipeDown(listener as EventHandler) }

    static void onZoomStarted(Node self, Closure listener) { self.setOnZoomStarted(listener as EventHandler) }

    static void onZoomFinished(Node self, Closure listener) { self.setOnZoomFinished(listener as EventHandler) }

    static void onContextMenuRequested(Node self, Closure listener) { self.setOnContextMenuRequested(listener as EventHandler) }

    static Subscription subscribe(ObservableValue self, Closure subscriber) {
        return self.subscribe(subscriber as Consumer)
    }

    static Subscription subscribe(ObservableValue self, Runnable subscriber) {
        return self.subscribe(subscriber)
    }
}
