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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationView extends View {
    // Arrow annotations have the highest z-index, then loupe, then blur
    private static final Annotation.Type[] TYPE_Z_INDEX = new Annotation.Type[] {
            Annotation.Type.ARROW,
            Annotation.Type.LOUPE,
            Annotation.Type.BLUR
    };

    private Bitmap mImage;
    private final Paint mImagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private Matrix mSharedMatrix = new Matrix();
    private Map<Annotation.Type, Set<Annotation>> mAnnotations = new ArrayMap<>();

    private Annotation mSelectedAnnotation; // Annotation user selected from toolbar
    private Annotation mCurrentAnnotation; // Annotation that is currently being used
    private PointF mMovingTouchPoint = null;
    private PointF mMovingStartPoint = null;
    private PointF mMovingEndPoint = null;
    private PointF mMultiTouch0 = null;
    private PointF mMultiTouch1 = null;
    private boolean mTouchesFlipped = false;

    public AnnotationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setImage(@NonNull Bitmap image) {
        mImage = image;
    }

    public void setAnnotation(Annotation annotation) {
        mSelectedAnnotation = annotation;
    }

    public Bitmap captureDecoratedImage() {
        if (mImage == null) {
            throw new RuntimeException("Image is null, nothing to capture!");
        }

        Bitmap output = mImage.copy(mImage.getConfig(), true);
        Canvas canvas = new Canvas(output);

        drawAnnotations(canvas);

        return output;
    }

    private void drawAnnotations(Canvas canvas) {
        // Iterate backwards because drawing happens bottom up.
        for (int i = TYPE_Z_INDEX.length - 1; i >= 0; i--) {
            Annotation.Type type = TYPE_Z_INDEX[i];
            Bitmap image = mImage;
            Set<Annotation> annotations = mAnnotations.get(type);
            if (annotations != null) {
                if (type == Annotation.Type.LOUPE && !CollectionUtils.isEmpty(mAnnotations.get(Annotation.Type.BLUR))) {
                    image = mImage.copy(mImage.getConfig(), true);
                    Canvas offScreenCanvas = new Canvas(image);
                    for (Annotation annotation : mAnnotations.get(Annotation.Type.BLUR)) {
                        annotation.render(offScreenCanvas, mImage);
                    }
                }
                for (Annotation annotation : annotations) {
                    annotation.render(canvas, image);
                }
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            return onMultitouchEvent(event);
        } else {
            return onSingleTouchEvent(event);
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mImage != null) {
            int defaultWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int defaultHeight = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            float defaultAspectRatio = (float) defaultWidth / (float) defaultHeight;
            float imageAspectRatio = (float) mImage.getWidth() / (float) mImage.getHeight();

            int width = defaultWidth;
            int height = defaultHeight;
            if (defaultAspectRatio > imageAspectRatio) {
                width = (int) (defaultHeight * imageAspectRatio);
            } else {
                height = (int) (defaultWidth / imageAspectRatio);
            }

            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        if (mImage == null) {
            // Skip drawing
            return;
        }

        mSharedMatrix.reset();
        float scaleX = (float) canvas.getWidth() / (float) mImage.getWidth();
        float scaleY = (float) canvas.getHeight() / (float) mImage.getHeight();
        mSharedMatrix.setScale(scaleX, scaleY);
        canvas.drawBitmap(mImage, mSharedMatrix, mImagePaint);

        drawAnnotations(canvas);
    }

    private boolean onSingleTouchEvent(MotionEvent event) {
        float canvasWidth = getMeasuredWidth();
        float canvasHeight = getMeasuredHeight();
        float pointX = event.getX();
        float pointY = event.getY();
        PointF point = new PointF(pointX, pointY);
        float percentX = pointX / canvasWidth;
        float percentY = pointY / canvasHeight;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Annotation existingAnnotation = getAnnotationAtPoint(point);

                if (existingAnnotation != null) {
                    // If we tapped on an existing annotation, start moving that
                    mCurrentAnnotation = existingAnnotation;
                    mMovingTouchPoint = point;
                    mMovingStartPoint = existingAnnotation.getStartPercentPoint().getAsPointF(canvasWidth, canvasHeight);
                    mMovingEndPoint = existingAnnotation.getEndPercentPoint().getAsPointF(canvasWidth, canvasHeight);
                } else {
                    // If we're drawing a new annotation, use whatever type is currently selected
                    mCurrentAnnotation = mSelectedAnnotation;
                    mCurrentAnnotation.setStartPercentPoint(percentX, percentY);
                }

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mCurrentAnnotation == null) {
                    break;
                }

                if (mMultiTouch0 != null && mMultiTouch1 != null) {
                    // If the user is using two fingers, ignore this gesture
                    break;
                }

                // If the annotation is being moved, set both the start & end point
                if (mMovingStartPoint != null) {
                    float deltaX = pointX - mMovingTouchPoint.x;
                    float deltaY = pointY - mMovingTouchPoint.y;
                    float newStartX = mMovingStartPoint.x + deltaX;
                    float newStartY = mMovingStartPoint.y + deltaY;
                    float newEndX = mMovingEndPoint.x + deltaX;
                    float newEndY = mMovingEndPoint.y + deltaY;

                    mCurrentAnnotation.setStartPercentPoint(newStartX / canvasWidth, newStartY / canvasHeight);
                    mCurrentAnnotation.setEndPercentPoint(newEndX / canvasWidth, newEndY / canvasHeight);
                } else {
                    // If we're creating a new annotation, then just set the end point
                    mCurrentAnnotation.setEndPercentPoint(percentX, percentY);
                    if (mAnnotations.containsKey(mCurrentAnnotation.getAnnotationType())) {
                        Set<Annotation> annotations = mAnnotations.get(mCurrentAnnotation.getAnnotationType());
                        annotations.add(mCurrentAnnotation);
                        mAnnotations.put(mCurrentAnnotation.getAnnotationType(), annotations);
                    } else {
                        Set<Annotation> annotations = new HashSet<>();
                        annotations.add(mCurrentAnnotation);
                        mAnnotations.put(mCurrentAnnotation.getAnnotationType(), annotations);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                mCurrentAnnotation = null;
                mMovingTouchPoint = null;
                mMovingStartPoint = null;
                mMovingEndPoint = null;
                break;
            default:
                return false;
        }

        invalidate();

        return true;
    }

    private boolean onMultitouchEvent(MotionEvent event) {
        final float canvasWidth = getMeasuredWidth();
        final float canvasHeight = getMeasuredHeight();
        final PointF touch0 = new PointF(event.getX(0), event.getY(0));
        final PointF touch1 = new PointF(event.getX(1), event.getY(1));

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mCurrentAnnotation = getAnnotationAtPoint(touch0);

                if (mCurrentAnnotation == null) {
                    break;
                }

                mMovingStartPoint = mCurrentAnnotation.getStartPercentPoint().getAsPointF(canvasWidth, canvasHeight);
                mMovingEndPoint = mCurrentAnnotation.getEndPercentPoint().getAsPointF(canvasWidth, canvasHeight);

                mMultiTouch0 = touch0;
                mMultiTouch1 = touch1;

                if (getDistance(touch0, mMovingEndPoint) < getDistance(touch1, mMovingEndPoint)) {
                    mTouchesFlipped = true;
                } else {
                    mTouchesFlipped = false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrentAnnotation == null) {
                    break;
                }

                float deltaX0 = touch0.x - mMultiTouch0.x;
                float deltaY0 = touch0.y - mMultiTouch0.y;
                float deltaX1 = touch1.x - mMultiTouch1.x;
                float deltaY1 = touch1.y -  mMultiTouch1.y;
                final float newStartX;
                final float newStartY;
                final float newEndX;
                final float newEndY;

                if (mTouchesFlipped) {
                    newStartX = mMovingStartPoint.x + deltaX1;
                    newStartY = mMovingStartPoint.y + deltaY1;
                    newEndX = mMovingEndPoint.x + deltaX0;
                    newEndY = mMovingEndPoint.y + deltaY0;
                } else {
                    newStartX = mMovingStartPoint.x + deltaX0;
                    newStartY = mMovingStartPoint.y + deltaY0;
                    newEndX = mMovingEndPoint.x + deltaX1;
                    newEndY = mMovingEndPoint.y + deltaY1;
                }

                mCurrentAnnotation.setStartPercentPoint(newStartX / canvasWidth, newStartY / canvasHeight);
                mCurrentAnnotation.setEndPercentPoint(newEndX / canvasWidth, newEndY / canvasHeight);
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mCurrentAnnotation = null;
                mMultiTouch0 = null;
                mMultiTouch1 = null;
                break;
        }

        invalidate();

        return true;
    }

    private @Nullable Annotation getAnnotationAtPoint(PointF point) {
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        for (Annotation.Type type : TYPE_Z_INDEX) {
            Set<Annotation> annotations = mAnnotations.get(type);
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    RectF rect = annotation.getRectF(viewWidth, viewHeight);
                    if (annotation.getAnnotationType() == Annotation.Type.LOUPE) {
                        float radius = annotation.getLength(viewWidth, viewHeight);
                        PointF center = annotation.getStartPercentPoint().getAsPointF(viewWidth, viewHeight);
                        float left = center.x - radius;
                        float top = center.y - radius;
                        float right = center.x + radius;
                        float bottom = center.y + radius;
                        rect = new RectF(left, top, right, bottom);
                    }

                    if (rect.contains(point.x, point.y)) {
                        return annotation;
                    }
                }
            }
        }

        return null;
    }

    private float getDistance(PointF p0, PointF p1) {
        return (float) Math.sqrt(Math.pow(p1.x - p0.x, 2) + Math.pow(p1.y - p0.y, 2));
    }
}
