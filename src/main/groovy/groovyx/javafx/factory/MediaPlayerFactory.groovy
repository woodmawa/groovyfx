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

import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

/**
 * JavaFX 2-era builder code (MediaPlayerBuilder) has been removed from modern JavaFX.
 * This factory now constructs Media/MediaPlayer directly and applies a small set of
 * commonly used attributes needed by the demos.
 */
class MediaPlayerFactory extends AbstractFXBeanFactory {

    MediaPlayerFactory() {
        super(MediaPlayer)
    }

    MediaPlayerFactory(Class<MediaPlayer> beanClass) {
        super(beanClass)
    }

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes)
            throws InstantiationException, IllegalAccessException {

        // Allow passing an already-created MediaPlayer as the value.
        if (checkValue(name, value)) {
            return value
        }

        // Demo-style usage:
        //   mediaPlayer(source: videoURL, autoPlay: true)
        // Also accept 'media' as an alias.
        def src = value
        if (src == null && attributes != null) {
            src = attributes.remove('source')
            if (src == null) src = attributes.remove('media')
            if (src == null) src = attributes.remove('url')
            if (src == null) src = attributes.remove('uri')
        }

        Media media = null
        if (src != null) {
            if (src instanceof Media) {
                media = (Media) src
            } else {
                String uri = normalizeToUriString(src)
                media = new Media(uri)
            }
        } else {
            throw new IllegalArgumentException(
                    "mediaPlayer requires a media source (provide a value or 'source:'/'media:' attribute)"
            )
        }

        MediaPlayer mp = new MediaPlayer(media)
        applyMediaPlayerAttributes(mp, attributes)
        return mp
    }

    /**
     * Apply a minimal attribute set that the demos rely on.
     * (Other generic properties can still be set later via FXHelper.applyAttributes.)
     */
    private static void applyMediaPlayerAttributes(MediaPlayer mp, Map attributes) {
        if (attributes == null || attributes.isEmpty()) return

        def v

        v = attributes.remove('autoPlay')
        if (v != null) mp.autoPlay = v as boolean

        v = attributes.remove('mute')
        if (v != null) mp.mute = v as boolean

        v = attributes.remove('volume')
        if (v != null) mp.volume = (v as Number).doubleValue()

        v = attributes.remove('balance')
        if (v != null) mp.balance = (v as Number).doubleValue()

        v = attributes.remove('rate')
        if (v != null) mp.rate = (v as Number).doubleValue()

        // Common event hooks (Groovy closures are fine here)
        v = attributes.remove('onEndOfMedia')
        if (v != null) mp.onEndOfMedia = v

        v = attributes.remove('onError')
        if (v != null) mp.onError = v

        v = attributes.remove('onHalted')
        if (v != null) mp.onHalted = v

        v = attributes.remove('onPaused')
        if (v != null) mp.onPaused = v

        v = attributes.remove('onPlaying')
        if (v != null) mp.onPlaying = v

        v = attributes.remove('onReady')
        if (v != null) mp.onReady = v

        v = attributes.remove('onRepeat')
        if (v != null) mp.onRepeat = v

        v = attributes.remove('onStalled')
        if (v != null) mp.onStalled = v

        v = attributes.remove('onStopped')
        if (v != null) mp.onStopped = v
    }

    /**
     * Convert common “source” inputs into a URI string suitable for javafx.scene.media.Media.
     * - File -> file:/... URI
     * - URL/URI -> string form
     * - String host/path without scheme -> assumes https://
     */
    private static String normalizeToUriString(Object src) {
        if (src instanceof File) {
            return ((File) src).toURI().toString()
        }
        if (src instanceof URL || src instanceof URI) {
            return src.toString()
        }

        String s = src.toString()?.trim()
        if (!s) {
            throw new IllegalArgumentException("Empty media source")
        }

        // If it looks like a bare hostname/path and has no scheme, assume https.
        if (!s.contains(":/") && s.contains(".") && s.contains("/")) {
            s = "https://${s}"
        }
        return s
    }
}
