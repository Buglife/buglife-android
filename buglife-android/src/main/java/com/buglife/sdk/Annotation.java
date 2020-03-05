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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.TypedValue;

class Annotation {

    enum Type {
        ARROW, LOUPE, BLUR
    }

    private final @NonNull Type mAnnotationType;
    private PercentPoint mStartPercentPoint = new PercentPoint(0, 0);
    private PercentPoint mEndPercentPoint = new PercentPoint(0, 0);
    private AnnotationRenderer renderer;

    public static Annotation newArrowInstance() {
        Annotation annotation = new Annotation(Type.ARROW);
        int fillColor = Color.parseColor("#f00060");
        int strokeColor = Color.WHITE;
        annotation.renderer = new ArrowRenderer(fillColor, strokeColor);
        return annotation;
    }

    public static Annotation newLoupeInstance(Context context) {
        Annotation annotation = new Annotation(Type.LOUPE);
        float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());
        annotation.renderer = new LoupeRenderer(strokeWidth);
        return annotation;
    }

    public static Annotation newBlurInstance() {
        Annotation annotation = new Annotation(Type.BLUR);
        annotation.renderer = new BlurRenderer();
        return annotation;
    }

    private Annotation(@NonNull Type annotationType) {
        mAnnotationType = annotationType;
    }

    private Annotation(Annotation annotation) {
        mAnnotationType = annotation.getAnnotationType();
        renderer = annotation.renderer;
        mStartPercentPoint = new PercentPoint(annotation.mStartPercentPoint);
        mEndPercentPoint = new PercentPoint(annotation.mEndPercentPoint);
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

    public void render(Canvas canvas, Bitmap originalImage) {
        if (renderer == null) {
            return;
        }

        renderer.drawAnnotation(this, canvas, originalImage);
    }

    public Annotation copy() {
        return new Annotation(this);
    }
}
