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
import android.graphics.Canvas;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.buglife.sdk.Annotation;
import com.buglife.sdk.PercentPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * View that contains a collection of annotations of a certain type.
 */
public abstract class AnnotationView extends View {

    final private ArrayList<Annotation> mAnnotations = new ArrayList<>();

    public AnnotationView(Context context) {
        this(context, null);
    }

    public AnnotationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Annotation annotation : mAnnotations) {
            drawAnnotation(annotation, canvas);
        }
    }

    void addAnnotation(Annotation annotation) {
        mAnnotations.add(annotation);
    }

    List<Annotation> getAnnotations() {
        return new ArrayList<>(mAnnotations);
    }

    protected abstract void drawAnnotation(Annotation annotation, Canvas canvas);

    /**
     * Converts a PercentPoint to a PointF on the view.
     */
    PointF getPointFromPercentPoint(PercentPoint percentPoint) {
        return getPointFromPercentPoint(percentPoint, getWidth(), getHeight());
    }

    /**
     * Converts a PercentPoint to a PointF using the given width + height.
     */
    static PointF getPointFromPercentPoint(PercentPoint percentPoint, float width, float height) {
        return new PointF(percentPoint.x * width, percentPoint.y * height);
    }

    /**
     * Returns the length of the annotation using the view's size.
     */
    float getLength(Annotation annotation) {
        return getLength(annotation, getWidth(), getHeight());
    }

    static float getLength(Annotation annotation, int width, int height) {
        PointF a = getPointFromPercentPoint(annotation.getStartPercentPoint(), width, height);
        PointF b = getPointFromPercentPoint(annotation.getEndPercentPoint(), width, height);
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }
}
