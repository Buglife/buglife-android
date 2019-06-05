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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.buglife.sdk.InvocationMethod;
import com.buglife.sdk.ActivityUtils;

import java.security.Permission;
import java.util.Date;

/**
 * Represents a "snapshot" of the current environment the moment that the bug reporter is invoked.
 */
public final class EnvironmentSnapshot implements Parcelable {
    private final float mBatteryLevel;
    private final long mFreeMemoryBytes;
    private final long mTotalMemoryBytes;
    private final long mFreeCapacityBytes;
    private final long mTotalCapacityBytes;
    @Nullable private final String mCarrierName;
    private final int mMobileNetworkSubtype;
    private final boolean mWifiConnected;
    @Nullable private final String mLocale;
    @NonNull private final Date mInvokedAt;
    @NonNull private final InvocationMethod mInvocationMethod;
    @Nullable private final Location mLocation;

    @SuppressLint("MissingPermission")
    public EnvironmentSnapshot(Context mContext, @NonNull InvocationMethod invocationMethod, boolean shouldCollectLocation) {
        mBatteryLevel = EnvironmentUtils.getBatteryLevel(mContext);
        ActivityManager.MemoryInfo memoryInfo = EnvironmentUtils.getMemoryInfo(mContext);
        mFreeMemoryBytes = memoryInfo.availMem;
        mTotalMemoryBytes = memoryInfo.totalMem;

        StatFs externalStats = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mTotalCapacityBytes = externalStats.getBlockSizeLong() * externalStats.getBlockCountLong();
            mFreeCapacityBytes = externalStats.getBlockSizeLong() * externalStats.getAvailableBlocksLong();
        } else {
            mTotalCapacityBytes = externalStats.getBlockSize() * externalStats.getBlockCount();
            mFreeCapacityBytes = externalStats.getBlockSize() * externalStats.getAvailableBlocks();
        }

        Connectivity connectivity = new Connectivity.Builder(mContext).build();
        mCarrierName = connectivity.getCarrierName();
        mMobileNetworkSubtype = connectivity.getMobileConnectionSubtype();
        mWifiConnected = connectivity.isConnectedToWiFi();
        mLocale = EnvironmentUtils.getLocale(mContext).toString();
        mInvokedAt = new Date();
        mInvocationMethod = invocationMethod;

        LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        Location tempLocation = null;
        if (shouldCollectLocation) {
            String[] fineLoc = {Manifest.permission.ACCESS_FINE_LOCATION};
            if (ActivityUtils.arePermissionsGranted(mContext, fineLoc)) {
                tempLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (tempLocation == null) {
                    // Fall back to network provider
                    tempLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } else {
                String[] coarseLoc = {Manifest.permission.ACCESS_COARSE_LOCATION};
                if (ActivityUtils.arePermissionsGranted(mContext, coarseLoc)) {
                    tempLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        }
        mLocation = tempLocation;
    }

    public float getBatteryLevel() {
        return mBatteryLevel;
    }

    public long getFreeMemoryBytes() {
        return mFreeMemoryBytes;
    }

    public long getTotalMemoryBytes() {
        return mTotalMemoryBytes;
    }

    public long getFreeCapacityBytes() {
        return mFreeCapacityBytes;
    }

    public long getTotalCapacityBytes() {
        return mTotalCapacityBytes;
    }

    @Nullable public String getCarrierName() {
        return mCarrierName;
    }

    public int getMobileNetworkSubtype() {
        return mMobileNetworkSubtype;
    }

    public boolean getWifiConnected() {
        return mWifiConnected;
    }

    @Nullable public String getLocale() {
        return mLocale;
    }

    public Date getInvokedAt() {return  mInvokedAt; }

    @NonNull public InvocationMethod getInvokationMethod() { return mInvocationMethod; }

    @Nullable public Location getLocation() {
        return mLocation;
    }

    /* Parcelable */

    EnvironmentSnapshot(Parcel in) {
        mBatteryLevel = in.readFloat();
        mFreeMemoryBytes = in.readLong();
        mTotalMemoryBytes = in.readLong();
        mFreeCapacityBytes = in.readLong();
        mTotalCapacityBytes = in.readLong();
        mCarrierName = in.readString();
        mMobileNetworkSubtype = in.readInt();
        mWifiConnected = in.readByte() != 0;
        mLocale = in.readString();
        mInvokedAt = (Date) in.readSerializable();
        mInvocationMethod = InvocationMethod.valueOf(in.readInt());
        mLocation = in.readParcelable(Location.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mBatteryLevel);
        dest.writeLong(mFreeMemoryBytes);
        dest.writeLong(mTotalMemoryBytes);
        dest.writeLong(mFreeCapacityBytes);
        dest.writeLong(mTotalCapacityBytes);
        dest.writeString(mCarrierName);
        dest.writeInt(mMobileNetworkSubtype);
        dest.writeByte((byte) (mWifiConnected ? 1 : 0));
        dest.writeString(mLocale);
        dest.writeSerializable(mInvokedAt);
        dest.writeInt(mInvocationMethod.getValue());
        dest.writeParcelable(mLocation, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EnvironmentSnapshot> CREATOR = new Creator<EnvironmentSnapshot>() {
        @Override
        public EnvironmentSnapshot createFromParcel(Parcel in) {
            return new EnvironmentSnapshot(in);
        }

        @Override
        public EnvironmentSnapshot[] newArray(int size) {
            return new EnvironmentSnapshot[size];
        }
    };
}
