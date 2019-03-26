package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;

public class ReportApp {

    @NonNull String bundleVersion;
    @NonNull String bundleIdentifier;
    @NonNull String bundleName;
    @NonNull String platform;

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
        return "ReportApp{" +
                "bundleVersion='" + bundleVersion + '\'' +
                ", bundleIdentifier='" + bundleIdentifier + '\'' +
                ", bundleName='" + bundleName + '\'' +
                ", platform='" + platform + '\'' +
                '}';
    }
}
