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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

final class BlurRenderer implements AnnotationRenderer {
    private static final float BLUR_RADIUS = 40;

    @Override
    public void drawAnnotation(Annotation annotation, Canvas canvas, Bitmap image) {
        Rect inBounds = annotation.getRect(image.getWidth(), image.getHeight());
        Rect outBounds = annotation.getRect(canvas.getWidth(), canvas.getHeight());

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        BlurRenderer.draw(image, inBounds, canvas, outBounds, paint);

        Paint borderPaint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        canvas.drawRect(outBounds, paint);
    }

    /**
     * Draws a blur effect on the provided canvas,
     */
    static void draw(@NonNull Bitmap in, @Nullable Rect inBounds, @NonNull Canvas canvas, @NonNull Rect outBounds, @NonNull Paint paint) {
        int inWidth = inBounds == null ? in.getWidth() : inBounds.width();
        int inHeight = inBounds == null ? in.getHeight() : inBounds.height();
        int inX = inBounds == null ? 0 : inBounds.left;
        int inY = inBounds == null ? 0 : inBounds.top;
        float scaleX = ((float) outBounds.width()) / inWidth;
        float scaleY = ((float) outBounds.height()) / inHeight;

        canvas.save();
        canvas.clipRect(outBounds);
        canvas.translate(outBounds.left, outBounds.top);
        canvas.scale(scaleX, scaleY);

        int cols = (int) (inWidth / BLUR_RADIUS + 1);
        int rows = (int) (inHeight / BLUR_RADIUS + 1);
        float halfSize = BLUR_RADIUS / 2f;
        final int bitmapWidth = in.getWidth();
        final int bitmapHeight = in.getHeight();
        final int canvasWidth = canvas.getWidth();
        final int canvasHeight = canvas.getHeight();

        for (int row = 0; row <= rows; row++ ) {
            float y = (row - 0.5f) * BLUR_RADIUS;
            float pixelY = inY + Math.max(Math.min(y, inHeight - 1), 0);

            for (int col = 0; col <= cols; col++ ) {
                float x = (col - 0.5f) * BLUR_RADIUS;
                float pixelX = inX + Math.max(Math.min(x, inWidth - 1), 0);

                int colorPixelX = (int) pixelX;
                int colorPixelY = (int) pixelY;

                if (colorPixelX < 0) {
                    colorPixelX = 1;
                }

                if (colorPixelY < 0) {
                    colorPixelY = 1;
                }

                if (colorPixelX >= bitmapWidth) {
                    colorPixelX = bitmapWidth - 1;
                }

                if (colorPixelY >= bitmapHeight) {
                    colorPixelY = bitmapHeight - 1;
                }

                paint.setColor(getPixelColor(in, colorPixelX, colorPixelY));
                canvas.drawRect(x - halfSize, y - halfSize, x + halfSize, y + halfSize, paint);
            }
        }

        canvas.restore();
    }

    private static @ColorInt int getPixelColor(@NonNull Bitmap pixels, int pixelX, int pixelY) {
        int pixel = pixels.getPixel(pixelX, pixelY);
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int alpha = (int) (1.0f * Color.alpha(pixel));
        return Color.argb(alpha, red, green, blue);
    }
}
