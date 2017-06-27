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

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

class Screenshotter {
    private final View mRootView;

    Screenshotter(Activity activity) {
        mRootView = activity.getWindow().getDecorView().getRootView();
    }

    Screenshotter(View view) {
        mRootView = view;
    }

    Bitmap getBitmap() {
        boolean drawingCacheWasEnabled = mRootView.isDrawingCacheEnabled();

        if (drawingCacheWasEnabled) {
            Bitmap sourceBitmap = mRootView.getDrawingCache();
            Bitmap copiedBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), false);
            return copiedBitmap;
        } else {
            mRootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(mRootView.getDrawingCache());
            mRootView.setDrawingCacheEnabled(drawingCacheWasEnabled);
            return bitmap;
        }
    }
}
