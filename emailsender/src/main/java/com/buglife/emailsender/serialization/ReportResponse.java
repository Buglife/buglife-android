package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;

/**
 * Defines details for buglife report
 */
// Using deserializer to populate
public class ReportResponse {
    @NonNull private ReportData report;
    @NonNull private ReportApp app;
    @NonNull private String email;

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

    @Nullable public static ReportResponse fromJson(@NonNull JSONObject jsonObject) {
        try {
            return Deserializer.getInstance()
                    .readValue(new StringReader(jsonObject.toString()), ReportResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //endregion

}
