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
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

class InvocationMethodManager {
    private final Activity mAttachedActivity;
    private final OnInvocationMethodTriggeredListener mListener;
    private boolean mRunning = false;
    @Nullable private ScreenshotObserver mScreenshotObserver;
    @Nullable private SensorManager mSensorManager;
    @Nullable private OnScreenshotTakenListener mScreenshotListener;
    @Nullable private ShakeDetector mShakeDetector;

    InvocationMethodManager(@NonNull Activity activity, @NonNull OnInvocationMethodTriggeredListener listener) {
        mAttachedActivity = activity;
        mListener = listener;
    }

    public void start(InvocationMethod invocationMethod) {
        if (mRunning) {
            Log.w("An invocation method is already running!");
            return;
        }
        setScreenshotInvocationMethodEnabled(invocationMethod == InvocationMethod.SCREENSHOT);
        setShakeInvocationMethodEnabled(invocationMethod == InvocationMethod.SHAKE);
        mRunning = true;
    }

    public void stop() {
        setScreenshotInvocationMethodEnabled(false);
        setShakeInvocationMethodEnabled(false);
        mRunning = false;
    }

    private void setShakeInvocationMethodEnabled(boolean enabled) {
        if (enabled) {
            setUpShakeDetector();
        } else {
            tearDownShakeDetector();
        }
    }

    private void setScreenshotInvocationMethodEnabled(boolean enabled) {
        if (enabled) {
            setUpScreenshotObserver();
        } else {
            tearDownScreenshotObserver();
        }
    }

    private void setUpShakeDetector() {
        mSensorManager = (SensorManager) mAttachedActivity.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager == null) {
            throw new RuntimeException("Unable to obtain SensorManager!");
        }

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector(new ShakeDetector.OnShakeListener() {
            @Override public void onShake() {
                SharedPreferences settings = mAttachedActivity.getSharedPreferences(ParametersActivity.PREFS_PARAMETERS_NAME, 0);
                boolean shake_params = settings.getBoolean(ParametersActivity.PREFS_SHAKE_PARAMETER_NAME, true);
                if (shake_params) mListener.onShakeInvocationMethodTriggered();
            }
        }, mAttachedActivity);

        boolean registered = mSensorManager.registerListener(mShakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (registered) {
            Log.d("Starting shake invocation method!");
        } else {
            Log.e("Unable to register shake listener");
        }
    }

    private void tearDownShakeDetector() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
            mSensorManager = null;
            mShakeDetector = null;
            Log.d("Stopping shake invocation method!");
        }
    }

    private void setUpScreenshotObserver() {
        if (mScreenshotListener == null) {
            mScreenshotListener = new OnScreenshotTakenListener() {
                @Override public void onScreenshotTaken(File file) {
                    if (file.mkdirs())
                        mListener.onScreenshotInvocationMethodTriggered(file);
                    else
                        Log.e("You should add android:requestLegacyExternalStorage=\"true\" into your AndroidManifest.xml");
                }
            };
        }
        mScreenshotObserver = ScreenshotObserver.Factory.newInstance(mAttachedActivity, mScreenshotListener);

        // Try to start the screenshot observer. However, if permission is denied by the user,
        // then disable screenshot invocations. This way, the user doesn't repeatedly get prompted to
        // grant permissions, but screenshot invocations can still be re-enabled programattically in the same session.
        mScreenshotObserver.start(mAttachedActivity, new ScreenshotObserver.ScreenshotObserverPermissionListener() {
            @Override
            public void onPermissionDenied() {
                start(InvocationMethod.NONE);
            }
        });

        Log.d("Starting screenshot invocation method!");
    }

    private void tearDownScreenshotObserver() {
        if (mScreenshotObserver != null) {
            mScreenshotObserver.stop();
            mScreenshotObserver = null;
            Log.d("Stopping screenshot invocation method!");
        }
    }

    interface OnInvocationMethodTriggeredListener {
        void onShakeInvocationMethodTriggered();
        void onScreenshotInvocationMethodTriggered(File file);
    }
}
