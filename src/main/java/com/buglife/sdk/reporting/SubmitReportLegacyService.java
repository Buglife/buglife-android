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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.android.volley.NoConnectionError;
import com.buglife.sdk.FileUtils;
import com.buglife.sdk.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class SubmitReportLegacyService extends IntentService {
    private SubmitReportTask mTask;

    public static void start(Context context) {
        context.startService(new Intent(context, SubmitReportLegacyService.class));
    }

    public SubmitReportLegacyService() {
        super("SubmitReportLegacyService");
    }

    @Override public void onCreate() {
        super.onCreate();
        mTask = new SubmitReportTask(getApplicationContext());
    }

    @Override protected void onHandleIntent(@Nullable Intent intent) {
        File cacheFile = FileUtils.getReportsCacheFile(getApplicationContext());
        if (!cacheFile.exists()) {
            Log.i("Reports cache file doesn't exist! No reports to submit.");
            return;
        }

        List<String> jsonReports = FileUtils.readLinesFromFile(cacheFile);
        Iterator<String> iterator = jsonReports.iterator();
        while (iterator.hasNext()) {
            String jsonReport = iterator.next();
            try {
                JSONObject report = new JSONObject(jsonReport);
                SubmitReportTask.Result result = mTask.execute(report);
                if (result.getError() != null) {
                    handleError(iterator, result.getError());
                } else {
                    handleSuccess(iterator);
                }
            } catch (JSONException error) {
                // If we can't deserialize the report, there's no point in retrying.
                iterator.remove();
                Log.e("Error deserializing JSON report!", error);
            }
        }

        if (jsonReports.isEmpty()) {
            cacheFile.delete();
        } else {
            FileUtils.writeLinesToFile(jsonReports, cacheFile);
        }
    }

    private void handleError(Iterator<String> jsonReportsIterator, Exception error) {
        if (shouldRemoveReportFromCache(error)) {
            jsonReportsIterator.remove();
        }
        Log.e("Error submitting report!", error);
    }

    private void handleSuccess(Iterator<String> jsonReportsIterator) {
        jsonReportsIterator.remove();
        Log.i("Report submitted!");
    }

    private boolean shouldRemoveReportFromCache(Exception error) {
        Throwable cause = error.getCause();
        return !(cause instanceof NoConnectionError);
    }
}
