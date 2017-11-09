/*
 * Copyright (C) 2017 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
