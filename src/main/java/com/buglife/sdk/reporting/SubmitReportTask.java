package com.buglife.sdk.reporting;

import android.app.job.JobParameters;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.buglife.sdk.NetworkManager;

import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SubmitReportTask extends AsyncTask<JobParameters, Void, SubmitReportTask.Result> {
    private static final String BUGLIFE_URL = "https://www.buglife.com/api/v1/reports.json";
    private final ResultCallback mCallback;
    private Context mContext;

    SubmitReportTask(ResultCallback callback) {
        mCallback = callback;
    }

    @Override protected Result doInBackground(JobParameters... jobParameters) {
        JobRepository jobRepository = new JobRepository();

        JobParameters jobParameter = jobParameters[0];
        int jobId = jobParameter.getJobId();
        JSONObject data = jobRepository.getJob(jobId);

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BUGLIFE_URL, data, future, future);
        NetworkManager.getInstance(mContext).addToRequestQueue(request);

        try {
            JSONObject response = future.get();
            return new Result(jobParameter, response);
        } catch (Exception error) {
            return new Result(jobParameter, error);
        }
    }

    @Override protected void onPostExecute(Result result) {
        if (result.getError() != null) {
            mCallback.onFailure(result.getJobParameters(), result.getError());
        } else if (result.getResponse() != null) {
            mCallback.onSuccess(result.getJobParameters(), result.getResponse());
        }
    }

    interface ResultCallback {
        void onSuccess(JobParameters jobParameters, JSONObject response);
        void onFailure(JobParameters jobParameters, Exception error);
    }

    class Result {
        private final JobParameters mJobParameters;
        private final JSONObject mResponse;
        private final Exception mError;

        Result(JobParameters jobParameters, JSONObject response) {
            mJobParameters = jobParameters;
            mResponse = response;
            mError = null;
        }

        Result(JobParameters jobParameters, Exception error) {
            mJobParameters = jobParameters;
            mResponse = null;
            mError = error;
        }

        JobParameters getJobParameters() {
            return mJobParameters;
        }

        JSONObject getResponse() {
            return mResponse;
        }

        Exception getError() {
            return mError;
        }
    }
}
