package com.buglife.sdk.reporting;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.buglife.sdk.Log;

import org.json.JSONException;
import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SubmitReportService extends JobService {
    public static final String KEY_DATA_PAYLOAD = "payload";

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, SubmitReportService.class);
    }

    @Override public boolean onStartJob(final JobParameters params) {
        try {
            String jsonReport = params.getExtras().getString(KEY_DATA_PAYLOAD);
            JSONObject report = new JSONObject(jsonReport);
            SubmitReportTask task = new SubmitReportTask(getApplicationContext(), new SubmitReportTask.ResultCallback() {
                @Override public void onSuccess(JSONObject response) {
                    jobFinished(params, false);
                }

                @Override public void onFailure(Exception error) {
                    Log.e("Failed to submit bug report", error);
                    jobFinished(params, false);
                }
            });
            task.execute(report);
            return true;
        } catch (JSONException e) {
            Log.e("Error deserializing JSON report", e);
        }
        return false;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
