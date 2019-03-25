package com.buglife.sdk.reporting;

import org.json.JSONObject;

public class SubmitReportResult {
    private final JSONObject mResponse;
    private final Exception mError;

    public SubmitReportResult(JSONObject response) {
        mResponse = response;
        mError = null;
    }

    public SubmitReportResult(Exception error) {
        mResponse = null;
        mError = error;
    }

    JSONObject getResponse() {
        return mResponse;
    }

    public Exception getError() {
        return mError;
    }
}
