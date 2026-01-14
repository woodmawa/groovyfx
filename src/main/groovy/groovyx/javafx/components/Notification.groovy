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
package groovyx.javafx.components


import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.util.Duration

/**
 * A non-blocking Notification (Toast) system.
 */
class Notification extends StackPane {
    static void show(Stage owner, String message, Duration duration = Duration.seconds(3)) {
        NotificationService.show(owner, message, duration)
    }
}
