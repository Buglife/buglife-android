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
import android.app.Application;
import android.os.Bundle;

class ForegroundDetector implements Application.ActivityLifecycleCallbacks {
    private Activity mCurrentActivity = null;
    private final OnForegroundListener mOnForegroundListener;

    interface OnForegroundListener {
        void onForegroundEvent();
        void onBackgroundEvent();
    }

    ForegroundDetector(Application application, OnForegroundListener onForegroundListener) {
        mOnForegroundListener = onForegroundListener;
        application.registerActivityLifecycleCallbacks(this);
    }

    Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    boolean getForegrounded() {
        return mCurrentActivity != null;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        mCurrentActivity = activity;
        mOnForegroundListener.onForegroundEvent();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mCurrentActivity = null;
        mOnForegroundListener.onBackgroundEvent();
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
