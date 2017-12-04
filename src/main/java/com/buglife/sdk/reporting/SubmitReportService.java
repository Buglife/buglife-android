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

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.buglife.sdk.IOUtils;
import com.buglife.sdk.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SubmitReportService extends JobService {
    public static final String KEY_EXTRA_REPORT_PATH = "report_path";

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, SubmitReportService.class);
    }

    @Override public boolean onStartJob(final JobParameters params) {
        try {
            String reportPath = params.getExtras().getString(KEY_EXTRA_REPORT_PATH);
            final File reportFile = new File(reportPath);
            String report = IOUtils.readStringFromFile(reportFile);
            JSONObject jsonReport = new JSONObject(report);
            SubmitReportAsyncTask task = new SubmitReportAsyncTask(getApplicationContext(), new SubmitReportAsyncTask.ResultCallback() {
                @Override public void onSuccess(JSONObject response) {
                    jobFinished(params, false);
                    reportFile.delete();
                }

                @Override public void onFailure(Exception error) {
                    Log.e("Error submitting report!", error);
                    jobFinished(params, false);
                }
            });
            task.execute(jsonReport);
            return true;
        } catch (JSONException e) {
            Log.e("Error deserializing JSON report!", e);
        }
        return false;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
