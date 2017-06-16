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

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.buglife.sdk.Log;

import java.util.List;

final class CarrierInfo {
    /**
     * At the moment, this always returns null because apps must request permission at runtime to
     * call getActiveSubscriptionInfoList()
     */
    static @Nullable String getCarrierName(Context context) {
//        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
//        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
//
//        if (subscriptionInfoList != null) {
//            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
//                CharSequence theCarrierName = subscriptionInfo.getCarrierName();
//                CharSequence theDisplayName = subscriptionInfo.getDisplayName();
//                Log.d("Carrier name = " + theCarrierName);
//                Log.d("Display name = " + theDisplayName);
//            }
//        }

        return null;
    }

    /**
     * At the moment, this always returns 0 because apps must request permission at runtime to
     * call getAllCellInfo()
     */
    static int getSignalStrength(Context context) {
//        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
//
//        if (cellInfoList != null && !cellInfoList.isEmpty()) {
//            CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfoList.get(0);
//
//            if (cellInfoGsm != null) {
//                CellSignalStrengthGsm cellSignalStrengthGsm = cellInfoGsm.getCellSignalStrength();
//                int asuLevel = cellSignalStrengthGsm.getAsuLevel();
//            }
//        }

        return 0;
    }
}
