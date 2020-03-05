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

/**
 * Logger used for internal Buglife logging.
 *
 * @warning Anything logged using this logger will NOT be collected in bug reports!
 *          If you'd like your logs to be collected in bug reports, use the Android system logger,
 *          or any other logging library.
 */
public final class Log {
    static final String TAG = "com.Buglife";

    public static void d(String msg) {
        android.util.Log.d(TAG, msg);
    }

    public static void d(String msg, Throwable throwable) {
        android.util.Log.d(TAG, msg, throwable);
    }

    public static void e(String msg) {
        android.util.Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable throwable) {
        android.util.Log.e(TAG, msg, throwable);
    }

    public static void i(String msg) {
        android.util.Log.i(TAG, msg);
    }

    public static void i(String msg, Throwable throwable) {
        android.util.Log.i(TAG, msg, throwable);
    }

    public static void w(String msg) {
        android.util.Log.w(TAG, msg);
    }

    public static void w(String msg, Throwable throwable) {
        android.util.Log.w(TAG, msg, throwable);
    }
}
