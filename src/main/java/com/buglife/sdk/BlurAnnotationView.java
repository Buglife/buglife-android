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
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.buglife.sdk.Annotation;
import com.buglife.sdk.AnnotationView;
import com.buglife.sdk.Blur;

/**
 * Annotation view that renders blur annotations.
 */
public final class BlurAnnotationView extends AnnotationView {

    private Bitmap mSourceBitmap;

    public BlurAnnotationView(Context context) {
        this(context, null, 0);
    }

    public BlurAnnotationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlurAnnotationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSourceBitamp(Bitmap bitmap) {
        mSourceBitmap = bitmap;
    }

    @Override
    protected void drawAnnotation(Annotation annotation, Canvas canvas) {
        Rect inBounds = annotation.getRect(mSourceBitmap.getWidth(), mSourceBitmap.getHeight());
        Rect outBounds = annotation.getRect(canvas.getWidth(), canvas.getHeight());

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        Blur.draw(mSourceBitmap, inBounds, canvas, outBounds, paint);

        Paint borderPaint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        canvas.drawRect(outBounds, paint);
    }
}
