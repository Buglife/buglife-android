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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.buglife.sdk.reporting.BugReporter;
import com.buglife.sdk.reporting.SubmitReportLegacyService;
import com.buglife.sdk.reporting.SubmitReportService;

import org.json.JSONException;
import org.json.JSONObject;

final class BugReporterImpl implements BugReporter {
    private final Context mContext;

    BugReporterImpl(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Kick off submit report service in case there are cached reports still
            // available.
            SubmitReportLegacyService.start(mContext);
        }
    }

    @Override public void report(Report report) {
        try {
            JSONObject jsonReport = report.toJSON();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                reportWithJobScheduler(jsonReport);
            } else {
                reportWithLegacy(jsonReport);
            }
        } catch (JSONException e) {
            Log.e("Failed to serialize bug report!", e);
            Toast.makeText(mContext, R.string.error_serialize_report, Toast.LENGTH_LONG).show();
        }
    }

    private void reportWithLegacy(JSONObject jsonReport) {
        String jsonReportString = jsonReport.toString();
        SubmitReportLegacyService.start(mContext, jsonReportString);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void reportWithJobScheduler(JSONObject jsonReport) {
        JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            throw new RuntimeException("Failed to obtain JobScheduler!");
        }

        int jobId = (int) System.currentTimeMillis();
        String jsonReportString = jsonReport.toString();

        PersistableBundle data = new PersistableBundle();
        data.putString(SubmitReportService.KEY_DATA_PAYLOAD, jsonReportString);

        JobInfo info = new JobInfo.Builder(jobId, SubmitReportService.getComponentName(mContext))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setExtras(data)
                .build();

        jobScheduler.schedule(info);
    }
}
