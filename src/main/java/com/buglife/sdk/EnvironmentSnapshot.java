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

package com.buglife.sdk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;
import android.support.annotation.Nullable;

import java.util.Locale;

/**
 * Represents a "snapshot" of the current environment the moment that the bug reporter is invoked.
 */
final class EnvironmentSnapshot implements Parcelable {
    private final float mBatteryLevel;
    private final long mFreeMemoryBytes;
    private final long mTotalMemoryBytes;
    private final long mFreeCapacityBytes;
    private final long mTotalCapacityBytes;
    @Nullable private final String mCarrierName;
    private final int mMobileNetworkSubtype;
    private final boolean mWifiConnected;
    @Nullable private final String mLocale;

    private EnvironmentSnapshot(float batteryLevel, long freeMemoryBytes, long totalMemoryBytes, long freeCapacityBytes, long totalCapacityBytes, String carrierName, int mobileNetworkSubtype, boolean wifiConnected, String locale) {
        mBatteryLevel= batteryLevel;
        mFreeMemoryBytes = freeMemoryBytes;
        mTotalMemoryBytes = totalMemoryBytes;
        mFreeCapacityBytes = freeCapacityBytes;
        mTotalCapacityBytes = totalCapacityBytes;
        mCarrierName = carrierName;
        mMobileNetworkSubtype = mobileNetworkSubtype;
        mWifiConnected = wifiConnected;
        mLocale = locale;
    }

    EnvironmentSnapshot(Parcel source) {
        mBatteryLevel = source.readFloat();
        mFreeMemoryBytes = source.readLong();
        mTotalMemoryBytes = source.readLong();
        mFreeCapacityBytes = source.readLong();
        mTotalCapacityBytes = source.readLong();
        mCarrierName = source.readString();
        mMobileNetworkSubtype = source.readInt();
        mWifiConnected = source.readInt() == 1;
        mLocale = source.readString();
    }

    float getBatteryLevel() {
        return mBatteryLevel;
    }

    long getFreeMemoryBytes() {
        return mFreeMemoryBytes;
    }

    long getTotalMemoryBytes() {
        return mTotalMemoryBytes;
    }

    long getFreeCapacityBytes() {
        return mFreeCapacityBytes;
    }

    long getTotalCapacityBytes() {
        return mTotalCapacityBytes;
    }

    @Nullable String getCarrierName() {
        return mCarrierName;
    }

    int getMobileNetworkSubtype() {
        return mMobileNetworkSubtype;
    }

    boolean getWifiConnected() {
        return mWifiConnected;
    }

    @Nullable String getLocale() {
        return mLocale;
    }

    @Override
    public int describeContents() {
        return 0;
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
        dest.writeInt(mWifiConnected ? 1 : 0);
        dest.writeString(mLocale);
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {

                @Override
                public EnvironmentSnapshot createFromParcel(Parcel source) {
                    return new EnvironmentSnapshot(source);
                }

                @Override
                public EnvironmentSnapshot[] newArray(int size) {
                    return new EnvironmentSnapshot[size];
                }
            };

    static class Builder {
        private final Context mContext;

        Builder(Context context) {
            mContext = context;
        }

        @SuppressWarnings("deprecation")
        EnvironmentSnapshot build() {
            float batteryLevel = getBatteryLevel();
            ActivityManager.MemoryInfo memoryInfo = getMemoryInfo();
            long freeMemoryBytes = memoryInfo.availMem;
            long totalMemoryBytes = memoryInfo.totalMem;

            StatFs externalStats = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long totalCapacityBytes;
            long freeCapacityBytes;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                totalCapacityBytes = externalStats.getBlockSizeLong() * externalStats.getBlockCountLong();
                freeCapacityBytes = externalStats.getBlockSizeLong() * externalStats.getAvailableBlocksLong();
            } else {
                totalCapacityBytes = externalStats.getBlockSize() * externalStats.getBlockCount();
                freeCapacityBytes = externalStats.getBlockSize() * externalStats.getAvailableBlocks();
            }

            Connectivity connectivity = new Connectivity.Builder(mContext).build();
            String carrierName = connectivity.getCarrierName();
            int mobileNetworkSubtype = connectivity.getMobileConnectionSubtype();
            boolean wifiConnected = connectivity.isConnectedToWiFi();
            Locale locale = getLocale(mContext);
            String localeString = (locale != null ? locale.toString() : null);

            return new EnvironmentSnapshot(batteryLevel, freeMemoryBytes, totalMemoryBytes, freeCapacityBytes, totalCapacityBytes, carrierName, mobileNetworkSubtype, wifiConnected, localeString);
        }

        private float getBatteryLevel() {
            Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            if (batteryIntent == null) {
                return 0;
            }

            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            // Error checking that probably isn't needed but I added just in case.
            if(level == -1 || scale == -1) {
                return 50.0f;
            }

            return ((float)level / (float)scale);
        }

        private ActivityManager.MemoryInfo getMemoryInfo() {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            return mi;
        }

        private static @Nullable Locale getLocale(Context context) {
            Configuration configuration = context.getResources().getConfiguration();

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                LocaleList localeList = configuration.getLocales();
//
//                if (!localeList.isEmpty()) {
//                    return localeList.get(0);
//                }
//            } else {
//                return configuration.locale;
//            }

            return configuration.locale;
        }
    }
}
