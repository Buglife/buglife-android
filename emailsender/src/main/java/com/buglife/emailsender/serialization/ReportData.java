package com.buglife.emailsender.serialization;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Using deserializer to populate
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportData {
    @NonNull private String whatHappened;
    @NonNull private String sdkVersion;
    @NonNull private String sdkName;
    @NonNull private String bundleVersion;
    @NonNull private String operatingSystemVersion;
    @NonNull private String deviceManufacturer;
    @NonNull private String deviceModel;
    @NonNull private String deviceBrand;
    @NonNull private String deviceIdentifier;
    private long totalCapacityBytes;
    private long freeCapacityBytes;
    private long freeMemoryBytes;
    private long totalMemoryBytes;
    private int batteryLevel;
    private int androidMobileNetworkSubtype;
    private boolean wifiConnected;
    @NonNull private String locale;
    private int invocationMethod;
    private int submissionAttempts;
    @NonNull private List<ReportAttachment> attachments;

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
        return "\nReportData{" +
                "\n\twhatHappened='" + whatHappened + '\'' +
                "\n\t, sdkVersion='" + sdkVersion + '\'' +
                "\n\t, sdkName='" + sdkName + '\'' +
                "\n\t, bundleVersion='" + bundleVersion + '\'' +
                "\n\t, operatingSystemVersion='" + operatingSystemVersion + '\'' +
                "\n\t, deviceManufacturer='" + deviceManufacturer + '\'' +
                "\n\t, deviceModel='" + deviceModel + '\'' +
                "\n\t, deviceBrand='" + deviceBrand + '\'' +
                "\n\t, deviceIdentifier='" + deviceIdentifier + '\'' +
                "\n\t, totalCapacityBytes=" + totalCapacityBytes +
                "\n\t, freeCapacityBytes=" + freeCapacityBytes +
                "\n\t, freeMemoryBytes=" + freeMemoryBytes +
                "\n\t, totalMemoryBytes=" + totalMemoryBytes +
                "\n\t, batteryLevel=" + batteryLevel +
                "\n\t, androidMobileNetworkSubtype=" + androidMobileNetworkSubtype +
                "\n\t, wifiConnected=" + wifiConnected +
                "\n\t, locale='" + locale + '\'' +
                "\n\t, invocationMethod=" + invocationMethod +
                "\n\t, submissionAttempts=" + submissionAttempts +
                "\n\t, attachments=" + attachments +
                "\n}";
    }
}
