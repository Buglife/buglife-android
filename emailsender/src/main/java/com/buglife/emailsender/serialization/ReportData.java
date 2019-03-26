package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// Using deserializer to populate
class ReportData {
    @SerializedName("what_happened") @NonNull String whatHappened;
    @NonNull String sdkVersion;
    @NonNull String sdkName;
    @NonNull String bundleVersion;
    @NonNull String operatingSystemVersion;
    @NonNull String deviceManufacturer;
    @NonNull String deviceModel;
    @NonNull String deviceBrand;
    @NonNull String deviceIdentifier;
    long totalCapacityBytes;
    long freeCapacityBytes;
    long freeMemoryBytes;
    long totalMemoryBytes;
    int batteryLevel;
    int androidMobileNetworkSubtype;
    boolean wifiConnected;
    @NonNull String locale;
    int invocationMethod;
    int submissionAttempts;
    @NonNull List<ReportAttachment> attachments;

    @NonNull
    public String getWhatHappened() {
        return whatHappened;
    }

    @NonNull
    public String getSdkVersion() {
        return sdkVersion;
    }

    @NonNull
    public String getSdkName() {
        return sdkName;
    }

    @NonNull
    public String getBundleVersion() {
        return bundleVersion;
    }

    @NonNull
    public String getOperatingSystemVersion() {
        return operatingSystemVersion;
    }

    @NonNull
    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    @NonNull
    public String getDeviceModel() {
        return deviceModel;
    }

    @NonNull
    public String getDeviceBrand() {
        return deviceBrand;
    }

    @NonNull
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public long getTotalCapacityBytes() {
        return totalCapacityBytes;
    }

    public long getFreeCapacityBytes() {
        return freeCapacityBytes;
    }

    public long getFreeMemoryBytes() {
        return freeMemoryBytes;
    }

    public long getTotalMemoryBytes() {
        return totalMemoryBytes;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public int getAndroidMobileNetworkSubtype() {
        return androidMobileNetworkSubtype;
    }

    public boolean isWifiConnected() {
        return wifiConnected;
    }

    @NonNull
    public String getLocale() {
        return locale;
    }

    public int getInvocationMethod() {
        return invocationMethod;
    }

    public int getSubmissionAttempts() {
        return submissionAttempts;
    }

    @NonNull
    public List<ReportAttachment> getAttachments() {
        return attachments;
    }

    @Override
    public String toString() {
        return "ReportData{" +
                "whatHappened='" + whatHappened + '\'' +
                ", sdkVersion='" + sdkVersion + '\'' +
                ", sdkName='" + sdkName + '\'' +
                ", bundleVersion='" + bundleVersion + '\'' +
                ", operatingSystemVersion='" + operatingSystemVersion + '\'' +
                ", deviceManufacturer='" + deviceManufacturer + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", deviceBrand='" + deviceBrand + '\'' +
                ", deviceIdentifier='" + deviceIdentifier + '\'' +
                ", totalCapacityBytes=" + totalCapacityBytes +
                ", freeCapacityBytes=" + freeCapacityBytes +
                ", freeMemoryBytes=" + freeMemoryBytes +
                ", totalMemoryBytes=" + totalMemoryBytes +
                ", batteryLevel=" + batteryLevel +
                ", androidMobileNetworkSubtype=" + androidMobileNetworkSubtype +
                ", wifiConnected=" + wifiConnected +
                ", locale='" + locale + '\'' +
                ", invocationMethod=" + invocationMethod +
                ", submissionAttempts=" + submissionAttempts +
                ", attachments=" + attachments +
                '}';
    }
}
