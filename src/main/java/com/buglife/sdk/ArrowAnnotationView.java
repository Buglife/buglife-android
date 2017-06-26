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

    private Paint mFillPaint;
    private Paint mStrokePaint;

    public ArrowAnnotationView(Context context) {
        this(context, null, 0);
    }

    public ArrowAnnotationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArrowAnnotationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawAnnotation(Annotation annotation, Canvas canvas) {
        PointF startPoint = getPointFromPercentPoint(annotation.getStartPercentPoint());
        PointF endPoint = getPointFromPercentPoint(annotation.getEndPercentPoint());

        float arrowLength = getLength(annotation);
        float tailWidth = getTailWidthForArrowLength(arrowLength);
        float headLength = getHeadLengthForArrowLength(arrowLength);
        float headWidth = getHeadWidthForArrowWithHeadLength(headLength);
        float strokeWidth = Math.max(1.0f, tailWidth * 0.25f);

        Path arrowPath = getArrowPath(startPoint, endPoint, arrowLength, tailWidth, headWidth, headLength);

        if (arrowPath != null) {
            Paint fillPaint = getFillPaint();
            Paint strokePaint = getStrokePaint();
            strokePaint.setStrokeWidth(strokeWidth);

            canvas.drawPath(arrowPath, strokePaint);
            canvas.drawPath(arrowPath, fillPaint);
        }
    }

    private Paint getFillPaint() {
        if (mFillPaint == null) {
            mFillPaint = new Paint();
            mFillPaint.setColor(getResources().getColor(R.color.arrow_annotation_fill_color));
            mFillPaint.setAntiAlias(true);
        }

        return mFillPaint;
    }

    private Paint getStrokePaint() {
        if (mStrokePaint == null) {
            mStrokePaint = new Paint();
            mStrokePaint.setColor(getResources().getColor(R.color.arrow_annotation_stroke_color));
            mStrokePaint.setAntiAlias(true);
            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
            mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
            mStrokePaint.setShadowLayer(8, 0, 0, Color.BLACK);

            // Allows the shadow to work
            setLayerType(LAYER_TYPE_SOFTWARE, mStrokePaint);
        }

        return mStrokePaint;
    }

    private static float getTailWidthForArrowLength(float arrowLength) {
        return Math.max(4.0f, arrowLength * 0.07f);
    }

    private static float getHeadLengthForArrowLength(float arrowLength) {
        return Math.max(arrowLength / 3.0f, 10.0f);
    }

    private static float getHeadWidthForArrowWithHeadLength(float headLength) {
        return (headLength * 0.9f);
    }

    private static @Nullable Path getArrowPath(PointF startPoint, PointF endPoint, float arrowLength, float tailWidth, float headWidth, float headLength) {
        if (arrowLength < 0.1) {
            return null;
        }

        float tailLength = arrowLength - headLength;

        Path path = new Path();
        path.moveTo(0, tailWidth / 2.0f);

        ArrayList<PointF> points = new ArrayList<>();
        points.add(new PointF(tailLength, tailWidth / 2.0f));
        points.add(new PointF(tailLength, headWidth / 2.0f));
        points.add(new PointF(arrowLength, 0));
        points.add(new PointF(tailLength, -headWidth / 2.0f));
        points.add(new PointF(tailLength, -tailWidth / 2.0f));
        points.add(new PointF(0, -tailWidth / 2.0f));

        for (PointF point : points) {
            path.lineTo(point.x, point.y);
        }

        path.close();

        float cosine = (endPoint.x - startPoint.x) / arrowLength;
        float sine = (endPoint.y - startPoint.y) / arrowLength;
        Matrix matrix = new Matrix();
        matrix.setValues(new float[] { cosine, -sine, startPoint.x, sine, cosine, startPoint.y, 0, 0, 1 });

        path.transform(matrix);

        return path;
    }
}
