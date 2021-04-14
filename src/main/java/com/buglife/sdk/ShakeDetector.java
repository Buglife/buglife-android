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
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

final class ShakeDetector implements SensorEventListener {

    // g-force required to register a shake event
    public static final float SHAKE_THRESHOLD = 2.5F;
    public static final int SHAKE_SLOP_TIME_MS = 500;
    public static final int SHAKE_COUNT_RESET_TIME_MS = 3000;
    public static final int MIN_SHAKE_COUNT = 2;

    private final OnShakeListener mOnShakeListener;
    private long mShakeTimestamp;
    private int mShakeCount;
    private SharedPreferences settingsPreferences;

    ShakeDetector(OnShakeListener onShakeListener, Activity activity) {
        mOnShakeListener = onShakeListener;
        settingsPreferences = activity.getSharedPreferences(ParametersActivity.PREFS_PARAMETERS_NAME, 0);
    }

    interface OnShakeListener {
        void onShake();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float)Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        float shake_threshold_params = settingsPreferences.getFloat(ParametersActivity.PREFS_SHAKE_THRESHOLD_PARAMETER_NAME, SHAKE_THRESHOLD);
        if (gForce > shake_threshold_params) {
            final long now = System.currentTimeMillis();
            // ignore shake events too close to each other
            int shake_slop_time_ms_params = settingsPreferences.getInt(ParametersActivity.PREFS_SHAKE_SLOP_TIME_MS_PARAMETER_NAME, SHAKE_SLOP_TIME_MS);
            if (mShakeTimestamp + shake_slop_time_ms_params > now) {
                return;
            }

            // reset the shake count after several seconds of no shakes
            int shake_count_reset_time_ms_params = settingsPreferences.getInt(ParametersActivity.PREFS_SHAKE_COUNT_RESET_TIME_MS_PARAMETER_NAME, SHAKE_COUNT_RESET_TIME_MS);
            if (mShakeTimestamp + shake_count_reset_time_ms_params < now) {
                mShakeCount = 0;
            }

            mShakeTimestamp = now;
            mShakeCount++;

            int shake_count_params = settingsPreferences.getInt(ParametersActivity.PREFS_MIN_SHAKE_COUNT_PARAMETER_NAME, MIN_SHAKE_COUNT);
            if (mShakeCount >= shake_count_params) {
                mShakeCount = 0;
                mOnShakeListener.onShake();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
