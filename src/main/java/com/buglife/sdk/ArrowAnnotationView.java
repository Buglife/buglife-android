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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.buglife.sdk.AnnotationView;
import com.buglife.sdk.R;
import com.buglife.sdk.Annotation;

import java.util.ArrayList;

/**
 * Annotation view that renders arrow annotations.
 */
public final class ArrowAnnotationView extends AnnotationView {

    private Paint mStrokePaint;

    public ArrowAnnotationView(Context context) {
        this(context, null, 0);
    }

    public ArrowAnnotationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArrowAnnotationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Allows the shadow to work
        setLayerType(LAYER_TYPE_SOFTWARE, mStrokePaint);
    }

    @Override
    protected void drawAnnotation(Annotation annotation, Canvas canvas) {
        int fillColor = getResources().getColor(R.color.arrow_annotation_fill_color);
        int strokeColor = getResources().getColor(R.color.arrow_annotation_stroke_color);
        ArrowRenderer arrowRenderer = new ArrowRenderer(fillColor, strokeColor);
        arrowRenderer.drawAnnotation(annotation, canvas);
    }
}
