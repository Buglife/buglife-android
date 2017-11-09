package com.buglife.sdk.reporting;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.buglife.sdk.NetworkManager;

import org.json.JSONObject;

final class SubmitReportTask {
    private static final String BUGLIFE_URL = "https://www.buglife.com/api/v1/reports.json";
    private final NetworkManager mNetworkManager;

    SubmitReportTask(Context context) {
        mNetworkManager = NetworkManager.getInstance(context);
    }

    Result execute(JSONObject report) {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BUGLIFE_URL, report, future, future);
        mNetworkManager.addToRequestQueue(request);

        try {
            JSONObject response = future.get();
            return new Result(response);
        } catch (Exception error) {
            return new Result(error);
        }
    }

    class Result {
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

        Exception getError() {
            return mError;
        }
    }
}
