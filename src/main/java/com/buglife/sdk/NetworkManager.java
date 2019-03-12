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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkManager {

    private static final int INITIAL_TIMEOUT_MS = 60 * 1000;
    public static final String BUGLIFE_URL = "https://www.buglife.com";


    private static NetworkManager mInstance;
    private OkHttpClient mOkHttpClient;

    private NetworkManager() {
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(INITIAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(INITIAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
    }

    public static synchronized NetworkManager getInstance() {
        if (mInstance == null) {
            mInstance = new NetworkManager();
        }

        return mInstance;
    }

    public Response executeRequest(Request request) throws IOException {
        return mOkHttpClient.newCall(request).execute();
    }

    public void executeRequestAsync(Request request, final Callback callback) {
        mOkHttpClient.newCall(request)
                     .enqueue(new Callback() {
                         @Override
                         public void onFailure(final Call call, final IOException e) {
                             callback.onFailure(call, e);
                         }

                         @Override
                         public void onResponse(final Call call, final Response response)
                                 throws IOException {
                             try {
                                 callback.onResponse(call, response);
                             } finally {
                                closeResponseBody(response);
                             }

                         }
                     });
    }

    private void closeResponseBody(Response response) {
        if (response.body() != null) {
            response.close();
        }
    }

}
