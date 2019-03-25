package com.buglife.sdk.reporting;

import org.json.JSONObject;

/**
 * Responsible of sending bug report to remote service
 */
public interface ISubmitReportTask {
    /**
     * Synchronously submits report
     * @param report input report
     * @return report submit result
     */
    SubmitReportResult execute(JSONObject report);

    /**
     * Asynchronously submits report
     * @param report input report
     * @param callback report status callback
     */
    void execute(JSONObject report, final ReportSubmissionCallback callback);
}
