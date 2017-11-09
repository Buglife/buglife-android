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
import com.buglife.sdk.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SubmitReportLegacyService extends IntentService {
    private static final String KEY_EXTRA_JSON_REPORT = "json_report";
    private SubmitReportTask mTask;

    public static void start(Context context, String jsonReport) {
        Intent intent = new Intent(context, SubmitReportLegacyService.class);
        intent.putExtra(KEY_EXTRA_JSON_REPORT, jsonReport);
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
        mTask = new SubmitReportTask(getApplicationContext());
    }

    @Override protected void onHandleIntent(@Nullable Intent intent) {
        File cacheFile = getReportsCacheFile(getApplicationContext());
        if (!cacheFile.exists()) {
            Log.i("Reports cache file doesn't exist! No reports to submit.");
            return;
        }

        List<String> pendingJsonReports = readLinesFromFile(cacheFile);

        if (intent != null && intent.hasExtra(KEY_EXTRA_JSON_REPORT)) {
            String newJsonReport = intent.getStringExtra(KEY_EXTRA_JSON_REPORT);
            pendingJsonReports.add(newJsonReport);
        }

        Iterator<String> iterator = pendingJsonReports.iterator();
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
        return !(cause instanceof NoConnectionError);
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
            closeQuietly(writer);
        }
    }

    private static List<String> readLinesFromFile(File file) {
        List<String> output = new ArrayList<>();
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
            closeQuietly(reader);
        }
        return output;
    }

    private static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException error) {
                // Ignore
            }
        }
    }
}
