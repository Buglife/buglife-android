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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class Screenshotter {
//    private final View mRootView;
    private final List<View> mRootViews;

    Screenshotter(Activity activity) {
        List<View> temp;
        temp = Screenshotter.getRootViews();
        if (temp == null || temp.size() == 0) {
        temp = new ArrayList<>();
        temp.add(activity.getWindow().getDecorView().getRootView());
        }
        mRootViews = temp;
    }

    Screenshotter(View view) {
        mRootViews = new ArrayList<>();
        mRootViews.add(view);
    }

    Bitmap getBitmap() {
        Bitmap flatteningCursor = Screenshotter.getBitmap(mRootViews.get(0));
        for (int i = 1; i < mRootViews.size(); i++) {
            flatteningCursor = Screenshotter.overlay(flatteningCursor, mRootViews.get(i));
        }
        return flatteningCursor;
    }

    // adapted from https://stackoverflow.com/questions/10616777/how-to-merge-to-two-bitmap-one-over-another
    private static Bitmap overlay(Bitmap bmp1, View view2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        Bitmap bmp2 = getBitmap(view2);
        if (bmp2 == null) {
            return bmOverlay;
        }
        int[] xy = new int[2];
        view2.getLocationOnScreen(xy);
        canvas.drawBitmap(bmp2, xy[0], xy[1], null);
        return bmOverlay;
    }

    private static Bitmap getBitmap(View view) {
        boolean drawingCacheWasEnabled = view.isDrawingCacheEnabled();

        if (drawingCacheWasEnabled) {
            Bitmap sourceBitmap = view.getDrawingCache();
            Bitmap copiedBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), false);
            return copiedBitmap;
        } else {
            view.setDrawingCacheEnabled(true);
            Bitmap sourceBitmap = view.getDrawingCache();
            if (sourceBitmap == null) { // I see this occasionally in the debugger, but not live :/
                Log.w("Unable to create bitmap from view: "+ view + " This view will not be part of the screenshot.");
                return null;
            }
            Bitmap copiedBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), false);
            view.setDrawingCacheEnabled(drawingCacheWasEnabled);
            return copiedBitmap;
        }

    }
    // adapted from https://stackoverflow.com/questions/19669984/is-there-a-way-to-programmatically-locate-all-windows-within-a-given-application
    private static List<View> getRootViews() {
        ArrayList<View> rootViews = new ArrayList<>();
        try {
            Class wmgClass = Class.forName("android.view.WindowManagerGlobal");
            Object wmgInstnace = wmgClass.getMethod("getInstance").invoke(null, (Object[])null);

            Method getViewRootNames = wmgClass.getMethod("getViewRootNames");
            Method getRootView = wmgClass.getMethod("getRootView", String.class);
            String[] rootViewNames = (String[])getViewRootNames.invoke(wmgInstnace, (Object[])null);

            for(String viewName : rootViewNames) {
                View rootView = (View)getRootView.invoke(wmgInstnace, viewName);
                rootViews.add(rootView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootViews;
    }

}
