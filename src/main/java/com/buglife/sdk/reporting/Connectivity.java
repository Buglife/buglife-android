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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import androidx.annotation.Nullable;
import android.telephony.TelephonyManager;

/**
 * Provides information about current network connectivity
 */
final class Connectivity {
    @Nullable private final NetworkInfo mWifiInfo;
    @Nullable private final NetworkInfo mMobileInfo;
    @Nullable private final String mCarrierName;

    Connectivity(@Nullable NetworkInfo wifiInfo, @Nullable NetworkInfo mobileInfo, @Nullable String carrierName) {
        mWifiInfo = wifiInfo;
        mMobileInfo = mobileInfo;
        mCarrierName = carrierName;
    }

    @Nullable String getCarrierName() {
        return mCarrierName;
    }

    boolean isConnectedToWiFi() {
        return mWifiInfo != null && mWifiInfo.isConnected();
    }

    boolean isConnectedToMobile() {
        return mMobileInfo != null && mMobileInfo.isConnected();
    }

    int getMobileConnectionSubtype() {
        if (mMobileInfo != null && isConnectedToMobile()) {
            return mMobileInfo.getSubtype();
        } else {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    static class Builder {
        private final Context mContext;

        Builder(Context context) {
            mContext = context;
        }

        Connectivity build() {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = null;
            NetworkInfo mobileInfo = null;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Network[] networks = connectivityManager.getAllNetworks();

                for (Network network : networks) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);

                    if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        wifiInfo = networkInfo;
                    } else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        mobileInfo = networkInfo;
                    }
                }
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    wifiInfo = networkInfo;
                } else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    mobileInfo = networkInfo;
                }
            }

            return new Connectivity(wifiInfo, mobileInfo, null);
        }
    }
}
