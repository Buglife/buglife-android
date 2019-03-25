package com.buglife.sdk.reporting;

import android.content.Context;
import android.support.annotation.NonNull;

public class SubmitReportProviderDefault implements SubmitReportProvider {

    @NonNull
    private final Context mContext;

    public SubmitReportProviderDefault(@NonNull Context context) {
        this.mContext = context;
    }

    @Override
    public ISubmitReportTask generateSubmitReport() {
        return new SubmitReportTask(mContext);
    }
}
