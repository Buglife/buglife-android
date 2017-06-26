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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.buglife.sdk.Annotation;
import com.buglife.sdk.AnnotationView;

/**
 * Annotation view that renders loupe annotations.
 */
public final class LoupeAnnotationView extends AnnotationView {

    private static final int MAGNIFICATION_FACTOR = 2;

    private Bitmap mSourceBitmap;
    private final Matrix mMatrix = new Matrix();
    private final Paint mBorderPaint = new Paint();

    public LoupeAnnotationView(Context context) {
        this(context, null);
    }

    public LoupeAnnotationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoupeAnnotationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        mBorderPaint.setStrokeWidth(strokeWidth);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(Color.BLACK);
    }

    void setSourceBitmap(Bitmap bitmap) {
        mSourceBitmap = bitmap;
    }

    @Override
    protected void drawAnnotation(Annotation annotation, Canvas canvas) {
        canvas.save();

        float radius = getLength(annotation);
        PointF center = getPointFromPercentPoint(annotation.getStartPercentPoint());
        Path loupePath = new Path();
        loupePath.addCircle(center.x, center.y, radius, Path.Direction.CW);
        canvas.clipPath(loupePath);

        mMatrix.reset();
        mMatrix.preScale(MAGNIFICATION_FACTOR, MAGNIFICATION_FACTOR);

        float px = center.x;
        float py = center.y;
        mMatrix.postTranslate(-px * (MAGNIFICATION_FACTOR - 1), -py * (MAGNIFICATION_FACTOR - 1));

        // Draw loupe contents
        canvas.drawBitmap(mSourceBitmap, mMatrix, null);
        // Draw loupe border
        canvas.drawCircle(center.x, center.y, radius, mBorderPaint);
        canvas.restore();
    }
}
