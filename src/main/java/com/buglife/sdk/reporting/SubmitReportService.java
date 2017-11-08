package com.buglife.sdk.reporting;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SubmitReportService extends JobService implements SubmitReportTask.ResultCallback {
    @Override public boolean onStartJob(JobParameters params) {
        SubmitReportTask task = new SubmitReportTask(this);
        task.execute(params);
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override public void onSuccess(JobParameters jobParameters, JSONObject response) {
        jobFinished(jobParameters, false);
    }

    @Override public void onFailure(JobParameters jobParameters, Exception error) {
        jobFinished(jobParameters, false);
    }
}
