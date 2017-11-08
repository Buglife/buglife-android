package com.buglife.sdk.reporting;

import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.buglife.sdk.NetworkManager;

import org.json.JSONObject;

public class SubmitReportTask extends AsyncTask<JSONObject, Void, SubmitReportTask.Result> {
    private static final String BUGLIFE_URL = "https://www.buglife.com/api/v1/reports.json";
    private final ResultCallback mCallback;
    private final Context mContext;

    SubmitReportTask(Context context, ResultCallback callback) {
        mContext = context;
        mCallback = callback;
    }

    @Override protected Result doInBackground(JSONObject... reports) {
        try {
            JSONObject report = reports[0];
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BUGLIFE_URL, report, future, future);
            NetworkManager.getInstance(mContext).addToRequestQueue(request);
            JSONObject response = future.get();
            return new Result(response);
        } catch (Exception error) {
            return new Result(error);
        }
    }

    @Override protected void onPostExecute(Result result) {
        if (result.getError() != null) {
            mCallback.onFailure(result.getError());
        } else if (result.getResponse() != null) {
            mCallback.onSuccess(result.getResponse());
        }
    }

    interface ResultCallback {
        void onSuccess(JSONObject response);
        void onFailure(Exception error);
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
