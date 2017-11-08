package com.buglife.sdk.reporting;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

public final class DeviceSnapshot implements Parcelable {
    private final String mOSVersion;
    private final String mDeviceManufacturer;
    private final String mDeviceModel;
    private final String mDeviceBrand;
    @Nullable private final String mDeviceIdentifier;

    public DeviceSnapshot() {
        mOSVersion = android.os.Build.VERSION.RELEASE;
        mDeviceManufacturer = Build.MANUFACTURER;
        mDeviceModel = Build.MODEL;
        mDeviceBrand = Build.BRAND;
        mDeviceIdentifier = null;
    }

    public String getOSVersion() {
        return mOSVersion;
    }

    public String getDeviceManufacturer() {
        return mDeviceManufacturer;
    }

    public String getDeviceModel() {
        return mDeviceModel;
    }

    public String getDeviceBrand() {
        return mDeviceBrand;
    }

    @Nullable
    public String getDeviceIdentifier() {
        return mDeviceIdentifier;
    }

    /* Parcelable */

    DeviceSnapshot(Parcel in) {
        mOSVersion = in.readString();
        mDeviceManufacturer = in.readString();
        mDeviceModel = in.readString();
        mDeviceBrand = in.readString();
        mDeviceIdentifier = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOSVersion);
        dest.writeString(mDeviceManufacturer);
        dest.writeString(mDeviceModel);
        dest.writeString(mDeviceBrand);
        dest.writeString(mDeviceIdentifier);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DeviceSnapshot> CREATOR = new Creator<DeviceSnapshot>() {
        @Override
        public DeviceSnapshot createFromParcel(Parcel in) {
            return new DeviceSnapshot(in);
        }

        @Override
        public DeviceSnapshot[] newArray(int size) {
            return new DeviceSnapshot[size];
        }
    };
}
