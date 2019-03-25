package com.buglife.emailsender;

import android.content.Context;
import android.support.annotation.NonNull;

import com.buglife.emailsender.entities.Creds;
import com.buglife.sdk.reporting.ISubmitReportTask;
import com.buglife.sdk.reporting.SubmitReportProvider;

public class SubmitReportProviderLocalEmail implements SubmitReportProvider {

    @NonNull private final Context context;
    @NonNull private final Creds credentials;

    public SubmitReportProviderLocalEmail(@NonNull Context context, @NonNull Creds credentials) {
        this.context = context;
        this.credentials = credentials;
    }

    @Override
    public ISubmitReportTask generateSubmitReport() {
        return new SubmitReportEmail(context, credentials);
    }

}
