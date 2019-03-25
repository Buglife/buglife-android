package com.buglife.sdk.reporting;

/**
 * Responsible for generating {@link ISubmitReportTask} whenever
 * it's needed to submit new report
 */
public interface SubmitReportProvider {
    /**
     * @return new generated {@link ISubmitReportTask}
     */
    ISubmitReportTask generateSubmitReport();
}
