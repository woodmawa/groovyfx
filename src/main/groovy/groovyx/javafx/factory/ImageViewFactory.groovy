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

import javafx.scene.image.Image
import javafx.scene.image.ImageView

class ImageViewFactory extends AbstractNodeFactory {

    ImageViewFactory() {
        super(ImageView)
    }

    ImageViewFactory(Class<ImageView> beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        ImageView iv = super.newInstance(builder, name, value, attributes)

        if (value != null) {
            iv.image = coerceToImage(value)
        }

        return iv
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child != null) {
            switch (child) {
                case Image:
                case File:
                case URL:
                case URI:
                    ((ImageView) parent).image = coerceToImage(child)
                    return
            }
        }
        super.setChild(builder, parent, child)
    }

    /**
     * Convert supported value types into a JavaFX Image, with URL normalization.
     */
    static Image coerceToImage(Object v) {
        switch (v) {
            case Image:
                return (Image) v

            case File:
                return new Image(((File) v).toURI().toString())

            case URL:
            case URI:
                return new Image(v.toString())

            default:
                String s = v.toString()?.trim()
                if (!s) return null

                // If it looks like a hostname/path and has no scheme, assume https.
                // (Fixes "images-assets.nasa.gov/image/..." style inputs.)
                if (!s.contains(":/") && s.contains(".") && s.contains("/")) {
                    s = "https://" + s
                }

                return new Image(s)
        }
    }
}
