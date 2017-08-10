package com.buglife.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnnotationView2 extends View {
    // Arrow annotations have the highest z-index, then loupe, then blur
    private static final Annotation.Type[] TYPE_Z_INDEX = new Annotation.Type[] {
            Annotation.Type.ARROW,
            Annotation.Type.LOUPE,
            Annotation.Type.BLUR
    };

    private Bitmap mImage;
    private Matrix mSharedMatrix = new Matrix();
    private Map<Annotation.Type, List<Annotation>> mAnnotations = new ArrayMap<>();

    private Annotation mCurrentAnnotation;
    private Annotation mMutatingAnnotation;
    private PointF mMovingTouchPoint = null;
    private PointF mMovingStartPoint = null;
    private PointF mMovingEndPoint = null;
    private PointF mMultiTouch0 = null;
    private PointF mMultiTouch1 = null;

    public AnnotationView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setImage(@NonNull Bitmap image) {
        mImage = image;
    }

    public void setCurrentAnnotation(Annotation annotation) {
        mCurrentAnnotation = annotation;
    }

    public Bitmap captureDecoratedImage() {
        if (mImage == null) {
            throw new RuntimeException("");
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
            List<Annotation> annotations = mAnnotations.get(type);
            if (annotations != null) {
                if (type == Annotation.Type.LOUPE && !mAnnotations.get(Annotation.Type.BLUR).isEmpty()) {
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
        canvas.drawBitmap(mImage, mSharedMatrix, null);

        drawAnnotations(canvas);
    }

    private boolean onSingleTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        float canvasWidth = getMeasuredWidth();
        float canvasHeight = getMeasuredHeight();
        float pointX = event.getX();
        float pointY = event.getY();
        PointF point = new PointF(pointX, pointY);
        float percentX = pointX / canvasWidth;
        float percentY = pointY / canvasHeight;

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                Annotation existingAnnotation = getAnnotationAtPoint(point);

                if (existingAnnotation != null) {
                    // If we tapped on an existing annotation, start moving that
                    mMutatingAnnotation = existingAnnotation;
                    mMovingTouchPoint = point;
                    mMovingStartPoint = AnnotationView.getPointFromPercentPoint(existingAnnotation.getStartPercentPoint(), canvasWidth, canvasHeight);
                    mMovingEndPoint = AnnotationView.getPointFromPercentPoint(existingAnnotation.getEndPercentPoint(), canvasWidth, canvasHeight);
                } else {
                    // If we're drawing a new annotation, use whatever type is currently selected
                    mMutatingAnnotation = mCurrentAnnotation;
                    mMutatingAnnotation.setStartPercentPoint(percentX, percentY);
                }

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mMutatingAnnotation == null) {
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

                    mMutatingAnnotation.setStartPercentPoint(newStartX / canvasWidth, newStartY / canvasHeight);
                    mMutatingAnnotation.setEndPercentPoint(newEndX / canvasWidth, newEndY / canvasHeight);
                } else {
                    // If we're creating a new annotation, then just set the end point
                    mMutatingAnnotation.setEndPercentPoint(percentX, percentY);
                    if (mAnnotations.containsKey(mMutatingAnnotation.getAnnotationType())) {
                        List<Annotation> annotations = mAnnotations.get(mMutatingAnnotation.getAnnotationType());
                        annotations.add(mMutatingAnnotation);
                        mAnnotations.put(mMutatingAnnotation.getAnnotationType(), annotations);
                    } else {
                        List<Annotation> annotations = new ArrayList<>();
                        annotations.add(mMutatingAnnotation);
                        mAnnotations.put(mMutatingAnnotation.getAnnotationType(), annotations);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                mMutatingAnnotation = null;
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
        throw new RuntimeException("Not implemented yet");
    }

    private @Nullable Annotation getAnnotationAtPoint(PointF point) {
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        for (Annotation.Type type : TYPE_Z_INDEX) {
            List<Annotation> annotations = mAnnotations.get(type);
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
}
