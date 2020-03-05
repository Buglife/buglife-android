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

package com.buglife.sdk.reporting;

import com.buglife.sdk.Log;
import com.buglife.sdk.NetworkManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class SubmitReportTask {
    private final NetworkManager mNetworkManager;
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BUGLIFE_REPORT_URL = NetworkManager.BUGLIFE_URL+"/api/v1/reports.json";

    public SubmitReportTask() {
        mNetworkManager = NetworkManager.getInstance();
    }

    /**
     * Synchronously executes a POST request
     * @param report a JSON payload with the report to submit
     * @return The result of the network request
     */
    public Result execute(JSONObject report) {
        final Request request = newRequest(report);

        try {
            final Response response = mNetworkManager.executeRequest(request);
            if (response.body() == null) {
                return new Result(new IllegalStateException("Response body was null!"));
            }

            final JSONObject responseJSONObject = new JSONObject(response.body().string());
            Log.d("Report submitted successfully!");
            return new Result(responseJSONObject);
        } catch (Exception error) {
            Log.d("Error submitting report", error);
            return new Result(error);
        }
    }

    /**
     * Asynchronously executes a POST request
     * @param report a JSON payload with the report to submit
     * @param callback Calls back with the result of the request; this is called on the main thread
     */
    public void execute(JSONObject report, final ReportSubmissionCallback callback) {
        mNetworkManager.executeRequestAsync(newRequest(report), new Callback() {
            @Override
            public void onFailure(final Call call, final IOException error) {
                Log.d("Error submitting report", error);
                callback.onFailure(ReportSubmissionCallback.Error.NETWORK, error);
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                Log.d("Report submitted successfully!");
                callback.onSuccess();
            }
        });
        Log.d("JSON object request for report added to request queue...");
    }

    private Request newRequest(JSONObject report) {
        return new Request.Builder()
                .url(BUGLIFE_REPORT_URL)
                .post(RequestBody.create(MEDIA_TYPE_JSON, report.toString()))
                .build();
    }

    public class Result {
        private final JSONObject mResponse;
        private final Exception mError;

        Result(JSONObject response) {
            mResponse = response;
            mError = null;
        }

        Result(Exception error) {
            mResponse = null;
            mError = error;
        }

        JSONObject getResponse() {
            return mResponse;
        }

        public Exception getError() {
            return mError;
        }
    }
}
