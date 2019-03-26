package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;

import org.json.JSONObject;

/**
 * Defines details for buglife report
 */
// Using deserializer to populate
public class ReportResponse {
    @NonNull ReportData report;
    @NonNull ReportApp app;
    @NonNull String email;

    @NonNull
    public ReportData getReport() {
        return report;
    }

    @NonNull
    public ReportApp getApp() {
        return app;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "ReportResponse{" +
                "report=" + report +
                ", app=" + app +
                ", email='" + email + '\'' +
                '}';
    }

    //region Factory

    public static ReportResponse fromJson(@NonNull JSONObject jsonObject) {
        return Deserializer.getInstance().fromJson(jsonObject.toString(), ReportResponse.class);
    }

    //endregion

}
