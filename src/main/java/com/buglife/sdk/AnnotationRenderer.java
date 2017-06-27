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

abstract class AnnotationRenderer {

    final static PointF getPointFromPercentPoint(PercentPoint percentPoint, float width, float height) {
        return new PointF(percentPoint.x * width, percentPoint.y * height);
    }

    final static float getLength(Annotation annotation, float width, float height) {
        PointF a = getPointFromPercentPoint(annotation.getStartPercentPoint(), width, height);
        PointF b = getPointFromPercentPoint(annotation.getEndPercentPoint(), width, height);
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }
}
