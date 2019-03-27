package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportApp {

    @NonNull private String bundleVersion;
    @NonNull private String bundleIdentifier;
    @NonNull private String bundleName;
    @NonNull private String platform;

    @NonNull
    public String getBundleVersion() {
        return bundleVersion;
    }

    @NonNull
    public String getBundleIdentifier() {
        return bundleIdentifier;
    }

    @NonNull
    public String getBundleName() {
        return bundleName;
    }

    @NonNull
    public String getPlatform() {
        return platform;
    }

    @Override
    public String toString() {
        return "\nReportApp{" +
                "\n\tbundleVersion='" + bundleVersion + '\'' +
                "\n\t, bundleIdentifier='" + bundleIdentifier + '\'' +
                "\n\t, bundleName='" + bundleName + '\'' +
                "\n\t, platform='" + platform + '\'' +
                "\n}";
    }
}
