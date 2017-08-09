package com.buglife.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.*;
import android.view.View;

public class AnnotationView2 extends View {
    private Bitmap mImage;
    private Matrix mSharedMatrix = new Matrix();

    public AnnotationView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setImage(@NonNull Bitmap image) {
        mImage = image;
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
    }
}
