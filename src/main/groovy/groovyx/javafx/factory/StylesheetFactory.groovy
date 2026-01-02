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

/**
 * StylesheetFactory - represents a stylesheet reference.
 *
 * Parents (SceneFactory/StageFactory) consume StylesheetRef and add it to
 * stylesheets collections.
 */
class StylesheetFactory extends AbstractFXBeanFactory {

    StylesheetFactory(Class beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        def url = value ?: attributes.remove("url") ?: attributes.remove("href") ?: attributes.remove("value")
        if (url == null) {
            throw new IllegalArgumentException(
                    "stylesheet requires a url/value (e.g., stylesheet('app.css') or stylesheet(url:'app.css'))"
            )
        }
        return new StylesheetRef(url.toString())
    }

    // Defensive: even if someone nests nodes under stylesheet { ... },
    // just ignore them instead of letting builder try to wire children.
    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        // no-op
    }
}

/** Wrapper so we don’t confuse CSS strings with other Strings (like stage title). */
class StylesheetRef {
    final String url
    StylesheetRef(String url) { this.url = url }
    @Override String toString() { url }
}
