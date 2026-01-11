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
package groovyx.javafx.factory

import groovy.util.FactoryBuilderSupport
import groovy.util.logging.Slf4j
import javafx.stage.Stage

@Slf4j
class PrimaryStageFactory extends StageFactory {

    PrimaryStageFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        super.onNodeCompleted(builder, parent, node)

        if (!(node instanceof Stage)) return

        builder.primaryStage = node
        log.debug("primaryStage explicitly assigned via primaryStage { }")
    }
}
