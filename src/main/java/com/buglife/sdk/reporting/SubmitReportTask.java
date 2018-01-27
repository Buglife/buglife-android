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

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.buglife.sdk.Log;
import com.buglife.sdk.NetworkManager;

import org.json.JSONObject;


public final class SubmitReportTask {
    private final NetworkManager mNetworkManager;
    private static final String BUGLIFE_REPORT_URL = NetworkManager.BUGLIFE_URL+"/api/v1/reports.json";

    public SubmitReportTask(Context context) {
        mNetworkManager = NetworkManager.getInstance(context);
    }

    /**
     * Synchronously executes a POST request
     * @param report a JSON payload with the report to submit
     * @return The result of the network request
     */
    public Result execute(JSONObject report) {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BUGLIFE_REPORT_URL, report, future, future);
        mNetworkManager.addToRequestQueue(request);
        Log.d("JSON object request for report added to request queue...");

        try {
            JSONObject response = future.get();
            Log.d("Report submitted successfully!");
            return new Result(response);
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
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BUGLIFE_REPORT_URL, report, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Report submitted successfully!");
                callback.onSuccess();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error submitting report", error);
                callback.onFailure(ReportSubmissionCallback.Error.NETWORK, error);
            }
        });
        mNetworkManager.addToRequestQueue(request);
        Log.d("JSON object request for report added to request queue...");
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
