package com.buglife.sdk.reporting;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

public class SubmitReportAsyncTask extends AsyncTask<JSONObject, Void, SubmitReportTask.Result> {
    private final ResultCallback mCallback;
    private final SubmitReportTask mTask;

    SubmitReportAsyncTask(Context context, ResultCallback callback) {
        mTask = new SubmitReportTask(context);
        mCallback = callback;
    }

    @Override protected SubmitReportTask.Result doInBackground(JSONObject... reports) {
        return mTask.execute(reports[0]);
    }

    @Override protected void onPostExecute(SubmitReportTask.Result result) {
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
}
