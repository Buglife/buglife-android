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

import com.buglife.sdk.Buglife;
import com.buglife.sdk.IOUtils;
import com.buglife.sdk.Log;
import com.buglife.sdk.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SubmitReportLegacyService extends IntentService {
    private static final String KEY_EXTRA_REPORT_PATH = "report_path";
    private ISubmitReportTask mTask;

    public static void start(Context context, File jsonReportFile) {
        Intent intent = new Intent(context, SubmitReportLegacyService.class);
        intent.putExtra(KEY_EXTRA_REPORT_PATH, jsonReportFile.getAbsolutePath());
        context.startService(intent);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, SubmitReportLegacyService.class);
        context.startService(intent);
    }

    public SubmitReportLegacyService() {
        super("SubmitReportLegacyService");
    }

    @Override public void onCreate() {
        super.onCreate();
        mTask = Buglife.getSubmitReportProvider().generateSubmitReport();
    }

    @Override protected void onHandleIntent(@Nullable Intent intent) {
        File cacheFile = getReportsCacheFile(getApplicationContext());
        List<String> pendingJsonReports = readLinesFromFile(cacheFile);

        if (intent != null && intent.hasExtra(KEY_EXTRA_REPORT_PATH)) {
            String reportPath = intent.getStringExtra(KEY_EXTRA_REPORT_PATH);
            File reportFile = new File(reportPath);
            try {
                String report = IOUtils.readStringFromFile(reportFile);
                pendingJsonReports.add(report);
                reportFile.delete();
            } catch (IOException e) {
                Log.e("Error reading report from disk!", e);
                Toast.makeText(getApplicationContext(), R.string.error_process_report, Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (pendingJsonReports.isEmpty()) {
            Log.i("No reports to submit.");
            return;
        }

        Iterator<String> iterator = pendingJsonReports.iterator();
        while (iterator.hasNext()) {
            String jsonReport = iterator.next();
            try {
                JSONObject report = new JSONObject(jsonReport);
                SubmitReportResult submitReportResult = mTask.execute(report);
                if (submitReportResult.getError() != null) {
                    handleError(iterator, submitReportResult.getError());
                } else {
                    handleSuccess(iterator);
                }
            } catch (JSONException error) {
                // If we can't deserialize the report, there's no point in retrying.
                iterator.remove();
                Log.e("Error deserializing JSON report!", error);
            }
        }

        if (pendingJsonReports.isEmpty()) {
            cacheFile.delete();
        } else {
            writeLinesToFile(pendingJsonReports, cacheFile);
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
        return !(cause instanceof ConnectException);
    }

    /* Utility methods */

    private static File getReportsCacheFile(Context context) {
        File dir = context.getFilesDir();
        return new File(dir, "reports");
    }

    private static void writeLinesToFile(List<String> lines, File file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (FileNotFoundException error) {
            error.printStackTrace();
        } catch (IOException error) {
            error.printStackTrace();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private static List<String> readLinesFromFile(File file) {
        List<String> output = new ArrayList<>();

        if (!file.exists()) {
            return output;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return output;
    }
}
