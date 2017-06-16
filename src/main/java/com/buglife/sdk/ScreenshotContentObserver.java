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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;

@TargetApi(Build.VERSION_CODES.M)
final class ScreenshotContentObserver implements ScreenshotObserver {
    private static final String TAG = "ScreenshotObserver";
    private static final String EXTERNAL_CONTENT_URI_PREFIX = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString();
    private static final String[] PROJECTION = new String[] {
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
    };
    private static final String SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC";
    private static final long DEFAULT_DETECT_WINDOW_SECONDS = 10;
    private static boolean sRequestedAndGrantedPermission = false;

    private final Context mAppContext;
    private final OnScreenshotTakenListener mListener;
    private ContentResolver mContentResolver = null;
    private ContentObserver mContentObserver = null;

    public ScreenshotContentObserver(Context appContext, OnScreenshotTakenListener listener) {
        mAppContext = appContext;
        mListener = listener;
    }

    private static boolean matchPath(String path) {
        return path.toLowerCase().contains("screenshot") || path.contains("截屏") || path.contains("截图");
    }

    private static boolean matchTime(long currentTime, long dateAdded) {
        return Math.abs(currentTime - dateAdded) <= DEFAULT_DETECT_WINDOW_SECONDS;
    }

    @Override
    public void start(final Activity currentActivity) {
        if (sRequestedAndGrantedPermission) {
            start();
            return;
        }

        FragmentManager fragmentManager = currentActivity.getFragmentManager();
        PermissionHelper permissionHelper = (PermissionHelper) fragmentManager.findFragmentByTag(PermissionHelper.TAG);

        if (permissionHelper == null) {
            permissionHelper = PermissionHelper.newInstance();
            permissionHelper.setPermissionCallback(new PermissionHelper.PermissionCallback() {
                @Override
                public void onPermissionGranted() {
                    sRequestedAndGrantedPermission = true;
                    start();
                }

                @Override
                public void onPermissionDenied() {
                    Toast.makeText(currentActivity, "Buglife needs read store permission to capture screenshots!", Toast.LENGTH_LONG).show();
                }
            });
            fragmentManager.beginTransaction().add(permissionHelper, PermissionHelper.TAG).commit();
        }
    }

    private void start() {
        mContentResolver = mAppContext.getContentResolver();
        mContentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.toString().startsWith(EXTERNAL_CONTENT_URI_PREFIX)) {
                    Cursor cursor = null;
                    String screenshotPath = null;

                    try {
                        cursor = mContentResolver.query(uri, PROJECTION, null, null, SORT_ORDER);

                        if (cursor != null && cursor.moveToFirst()) {
                            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                            long dateAdded = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
                            // Warning: If the user manually sets their system time to something else,
                            // this may not work
                            long currentTime = System.currentTimeMillis() / 1000;

                            if (matchPath(path) && matchTime(currentTime, dateAdded)) {
                                Log.d("Got screenshot: " + path);
                                screenshotPath = path;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Open cursor failed", e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    if (screenshotPath != null) {
                        File file = new File(screenshotPath);
                        mListener.onScreenshotTaken(file);
                    }
                }

                super.onChange(selfChange, uri);
            }
        };

        mContentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
    }

    @Override
    public void stop() {
        if (mContentResolver != null && mContentObserver != null) {
            mContentResolver.unregisterContentObserver(mContentObserver);
            mContentResolver = null;
            mContentObserver = null;
        }
    }
}
