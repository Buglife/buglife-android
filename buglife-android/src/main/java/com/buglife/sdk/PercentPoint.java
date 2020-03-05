/*
 * Copyright (C) 2017 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.sdk;

import android.graphics.PointF;

/**
 * Represents a point that is a given percentage across the screen.
 * This is basically just a PointF, except the values must always be between 0 and 1
 */
class PercentPoint extends PointF {
    PercentPoint(float x, float y) {
        super(x, y);
    }

    PercentPoint(PercentPoint percentPoint) {
        this.x = percentPoint.x;
        this.y = percentPoint.y;
    }

    public PointF getAsPointF(float width, float height) {
        return new PointF(x * width, y * height);
    }
}
