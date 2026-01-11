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
import javafx.stage.Popup
import javafx.stage.PopupWindow

class PopupFactory extends AbstractFXBeanFactory  {

    PopupFactory() {
        super(Popup)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        Popup p = new Popup()

        // Common PopupWindow attrs (optional)
        if (attributes.containsKey('autoHide')) {
            p.autoHide = attributes.remove('autoHide') as boolean
        }
        if (attributes.containsKey('hideOnEscape')) {
            p.hideOnEscape = attributes.remove('hideOnEscape') as boolean
        }
        if (attributes.containsKey('consumeAutoHidingEvents')) {
            p.consumeAutoHidingEvents = attributes.remove('consumeAutoHidingEvents') as boolean
        }

        // value isn't used; children get added via content factory / node handling
        return p
    }
}