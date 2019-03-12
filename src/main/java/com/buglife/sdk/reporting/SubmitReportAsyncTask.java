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

import org.json.JSONObject;

import android.os.AsyncTask;

public class SubmitReportAsyncTask extends AsyncTask<JSONObject, Void, SubmitReportTask.Result> {
    private final ResultCallback mCallback;
    private final SubmitReportTask mTask;

    SubmitReportAsyncTask(ResultCallback callback) {
        mTask = new SubmitReportTask();
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
