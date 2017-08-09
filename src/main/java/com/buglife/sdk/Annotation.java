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
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

class Annotation {

    enum Type {
        ARROW, LOUPE, BLUR
    }

    private final @NonNull Type mAnnotationType;
    private PercentPoint mStartPercentPoint = new PercentPoint(0, 0);
    private PercentPoint mEndPercentPoint = new PercentPoint(0, 0);

    Annotation(@NonNull Type annotationType) {
        mAnnotationType = annotationType;
    }

    PercentPoint getStartPercentPoint() {
        return mStartPercentPoint;
    }

    @NonNull Type getAnnotationType() {
        return mAnnotationType;
    }

    void setStartPercentPoint(PercentPoint startPercentPoint) {
        mStartPercentPoint = startPercentPoint;
    }

    void setStartPercentPoint(float x, float y) {
        mStartPercentPoint = new PercentPoint(x, y);
    }

    PercentPoint getEndPercentPoint() {
        return mEndPercentPoint;
    }

    void setEndPercentPoint(PercentPoint endPercentPoint) {
        mEndPercentPoint = endPercentPoint;
    }

    void setEndPercentPoint(float x, float y) {
        mEndPercentPoint = new PercentPoint(x, y);
    }

    private float getLeftPercent() {
        return Math.min(getStartPercentPoint().x, getEndPercentPoint().x);
    }

    private float getTopPercent() {
        return Math.min(getStartPercentPoint().y, getEndPercentPoint().y);
    }

    private float getRightPercent() {
        return Math.max(getStartPercentPoint().x, getEndPercentPoint().x);
    }

    private float getBottomPercent() {
        return Math.max(getStartPercentPoint().y, getEndPercentPoint().y);
    }

    Rect getRect(int width, int height) {
        Rect rect = new Rect();
        RectF rectF = getRectF(width, height);
        rectF.round(rect);
        return rect;
    }

    RectF getRectF(float width, float height) {
        float left = getLeftPercent() * width;
        float top = getTopPercent() * height;
        float right = getRightPercent() * width;
        float bottom = getBottomPercent() * height;
        return new RectF(left, top, right, bottom);
    }

    float getLength(int width, int height) {
        PointF a = getStartPercentPoint().getAsPointF(width, height);
        PointF b = getEndPercentPoint().getAsPointF(width, height);
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }
}
