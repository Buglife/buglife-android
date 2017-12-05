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
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.buglife.sdk.reporting.BugReporter;
import com.buglife.sdk.reporting.ReportSubmissionCallback;
import com.buglife.sdk.reporting.SubmitReportLegacyService;
import com.buglife.sdk.reporting.SubmitReportService;
import com.buglife.sdk.reporting.SubmitReportTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

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

    @Override public void report(Report report, ReportSubmissionCallback callback) {
        File reportFile;

        try {
            JSONObject jsonReport = report.toJSON();

            boolean forceSynchronous = (Buglife.getRetryPolicy() == RetryPolicy.MANUAL);

            if (forceSynchronous) {
                reportSynchronously(jsonReport, callback);
                return;
            }

            String filename = "buglife_report_" + System.currentTimeMillis() + ".json";
            reportFile = new File(mContext.getCacheDir(), filename);
            IOUtils.writeStringToFile(jsonReport.toString(), reportFile);
        } catch (JSONException e) {
            Log.e("Failed to serialize bug report!", e);
            callback.onFailure(ReportSubmissionCallback.Error.SERIALIZATION, e);
            return;
        } catch (IOException e) {
            Log.e("Failed to write bug report file!", e);
            callback.onFailure(ReportSubmissionCallback.Error.SERIALIZATION, e);
            return;
        }

        boolean forceLegacy = (Buglife.getRetryPolicy() == RetryPolicy.AUTOMATIC_LEGACY);

        if (!forceLegacy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d("Attempting to schedule report submission...");

            JobScheduler jobScheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            if (jobScheduler != null) {
                Log.d("Acquired JobScheduler service...");
                reportWithJobScheduler(reportFile, jobScheduler);
            } else {
                Log.e("JobScheduler unavailable; Falling back to legacy report submission.");
                reportWithLegacy(reportFile);
            }
        } else {
            Log.d("Submitting report using legacy service.");
            reportWithLegacy(reportFile);
        }

        callback.onSuccess();
    }

    private void reportWithLegacy(File reportFile) {
        SubmitReportLegacyService.start(mContext, reportFile);
    }


    private void reportSynchronously(JSONObject jsonReport, ReportSubmissionCallback callback) {
        SubmitReportTask task = new SubmitReportTask(mContext);
        task.execute(jsonReport, callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void reportWithJobScheduler(@NonNull File reportFile, @NonNull JobScheduler jobScheduler) {
        int jobId = (int) System.currentTimeMillis();
        PersistableBundle data = new PersistableBundle();
        data.putString(SubmitReportService.KEY_EXTRA_REPORT_PATH, reportFile.getAbsolutePath());

        JobInfo info = new JobInfo.Builder(jobId, SubmitReportService.getComponentName(mContext))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setExtras(data)
                .build();

        jobScheduler.schedule(info);
    }
}
