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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.buglife.sdk.Log;

public class SessionSnapshot implements Parcelable {
    private final String mPlatform;
    private final String mSDKVersion;
    private final String mSDKName;
    private final String mUserEmail;
    private final String mUserIdentifier;
    private final String mBundleIdentifier;
    private final String mBundleName;
    @Nullable private final String mBundleShortVersion;
    @Nullable private final String mBundleVersion;

    public SessionSnapshot(Context context, String userEmail, String userIdentifier) {
        mPlatform = "android";
        mSDKVersion = com.buglife.sdk.BuildConfig.VERSION_NAME;
        String sdkStr;
        try {
            Class.forName("com.buglife.BuglifeModule");
            sdkStr = "Buglife React Native Android";
        }
        catch (ClassNotFoundException e) {
            sdkStr = "Buglife Android";
        }
        mSDKName = sdkStr;
        mUserEmail = userEmail;
        mUserIdentifier = userIdentifier;
        mBundleIdentifier = context.getPackageName();

        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        mBundleName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(mBundleIdentifier, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Unable to get version information / package information", e);
        }

        if (packageInfo != null) {
            mBundleVersion = Integer.toString(packageInfo.versionCode);
            mBundleShortVersion = packageInfo.versionName;
        } else {
            mBundleVersion = null;
            mBundleShortVersion = null;
        }
    }

    public String getPlatform() {
        return mPlatform;
    }

    public String getSDKVersion() {
        return mSDKVersion;
    }

    public String getSDKName() {
        return mSDKName;
    }

    public String getUserEmail() {
        return mUserEmail;
    }

    public String getUserIdentifier() {
        return mUserIdentifier;
    }

    @Nullable
    public String getBundleShortVersion() {
        return mBundleShortVersion;
    }

    @Nullable
    public String getBundleVersion() {
        return mBundleVersion;
    }

    public String getBundleIdentifier() {
        return mBundleIdentifier;
    }

    public String getBundleName() {
        return mBundleName;
    }

    /* Parcelable */

    SessionSnapshot(Parcel in) {
        mPlatform = in.readString();
        mSDKVersion = in.readString();
        mSDKName = in.readString();
        mUserEmail = in.readString();
        mUserIdentifier = in.readString();
        mBundleShortVersion = in.readString();
        mBundleVersion = in.readString();
        mBundleIdentifier = in.readString();
        mBundleName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPlatform);
        dest.writeString(mSDKVersion);
        dest.writeString(mSDKName);
        dest.writeString(mUserEmail);
        dest.writeString(mUserIdentifier);
        dest.writeString(mBundleShortVersion);
        dest.writeString(mBundleVersion);
        dest.writeString(mBundleIdentifier);
        dest.writeString(mBundleName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SessionSnapshot> CREATOR = new Creator<SessionSnapshot>() {
        @Override
        public SessionSnapshot createFromParcel(Parcel in) {
            return new SessionSnapshot(in);
        }

        @Override
        public SessionSnapshot[] newArray(int size) {
            return new SessionSnapshot[size];
        }
    };
}
