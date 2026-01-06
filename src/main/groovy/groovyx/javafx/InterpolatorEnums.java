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

import javafx.animation.Interpolator;

/**
 * Convenience enum that exposes common JavaFX {@link Interpolator} instances.
 *
 * <p>This is primarily used to make interpolators easier to reference from GroovyFX DSL code.</p>
 *
 * @author jimclarke
 */
public enum InterpolatorEnums {
    /** Discrete interpolation (no in-between values). */
    DISCRETE(Interpolator.DISCRETE),

    /** Linear interpolation. */
    LINEAR(Interpolator.LINEAR),

    /** Ease-in/ease-out interpolation. */
    EASE_BOTH(Interpolator.EASE_BOTH),

    /** Ease-in interpolation. */
    EASE_IN(Interpolator.EASE_IN),

    /** Ease-out interpolation. */
    EASE_OUT(Interpolator.EASE_OUT);

    private final Interpolator interpolator;

    InterpolatorEnums(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    /**
     * Returns the underlying JavaFX {@link Interpolator} instance.
     *
     * @return the interpolator
     */
    public Interpolator interpolator() {
        return interpolator;
    }
}
