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
import android.support.annotation.NonNull;

import java.util.List;

final class BitmapAnnotator {

    static @NonNull Bitmap createBitmapWithBlurAnnotations(Bitmap sourceBitmap, List<Annotation> blurAnnotations) {
        final Bitmap destinationBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), true);
        final Canvas canvas = new Canvas(destinationBitmap);
        final BlurRenderer blurRenderer = new BlurRenderer(sourceBitmap);

        for (Annotation annotation : blurAnnotations) {
            blurRenderer.drawAnnotation(annotation, canvas);
        }

        return destinationBitmap;
    }

}
