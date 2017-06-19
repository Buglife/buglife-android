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
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;

import java.io.File;

/**
 * When running on anything prior to Android M, we use FileObserver to detect screenshots
 */
final class ScreenshotFileObserver extends FileObserver implements ScreenshotObserver {
    private static final String PATH = Environment.getExternalStorageDirectory().toString() + "/Pictures/Screenshots/";

    private String mLastTakenPath;
    private final OnScreenshotTakenListener mListener;

    public ScreenshotFileObserver(OnScreenshotTakenListener listener) {
        super(PATH, FileObserver.CLOSE_WRITE);
        mListener = listener;
    }

    @Override
    public void onEvent(int event, String path) {
        if (path == null || event != FileObserver.CLOSE_WRITE) {
            //Log.i(TAG, "Not important");
        } else if (mLastTakenPath != null && path.equalsIgnoreCase(mLastTakenPath)) {
            //Log.i(TAG, "This event has been observed before.");
        } else {
            mLastTakenPath = path;
            File file = new File(PATH + path);

            if (mListener != null) {
                mListener.onScreenshotTaken(file);
            }
        }
    }

    @Override
    public void start(Activity currentActivity) {
        startWatching();
    }

    @Override
    public void stop() {
        stopWatching();
    }
}
