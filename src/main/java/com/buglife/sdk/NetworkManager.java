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

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.Volley;

public class NetworkManager {

    private static final int INITIAL_TIMEOUT_MS = 60 * 1000;
    public static final String BUGLIFE_URL = "https://www.buglife.com";


    private static NetworkManager mInstance;
    private RequestQueue mRequestQueue;

    private NetworkManager(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public static synchronized NetworkManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new NetworkManager(context);
        }

        return mInstance;
    }

    public <T> void addToRequestQueue(Request<T> request) {
        // Volley's default initial timeout is way too short, so let's bump it up.
        RetryPolicy retryPolicy = new DefaultRetryPolicy(INITIAL_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        mRequestQueue.add(request);
    }
}
