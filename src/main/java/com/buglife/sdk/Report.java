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

import com.buglife.sdk.reporting.DeviceSnapshot;
import com.buglife.sdk.reporting.EnvironmentSnapshot;
import com.buglife.sdk.reporting.SessionSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Represents a bug report draft.
 */
public final class Report {
    private final BugContext mBugContext;

    Report(BugContext bugContext) {
        mBugContext = bugContext;
    }

    public JSONObject toJSON() throws JSONException {
        String whatHappened = mBugContext.getAttribute(TextInputField.SUMMARY_ATTRIBUTE_NAME);

        JSONObject params = new JSONObject();
        JSONObject reportParams = new JSONObject();
        JSONObject appParams = new JSONObject();

        reportParams.put("what_happened", whatHappened);

        SessionSnapshot sessionSnapshot = mBugContext.getSessionSnapshot();
        reportParams.put("sdk_version", sessionSnapshot.getSDKVersion());
        reportParams.put("sdk_name", sessionSnapshot.getSDKName());
        reportParams.put("user_email", sessionSnapshot.getUserEmail());
        reportParams.put("user_identifier", sessionSnapshot.getUserIdentifier());
        reportParams.put("bundle_short_version", sessionSnapshot.getBundleShortVersion());
        reportParams.put("bundle_version", sessionSnapshot.getBundleVersion());

        DeviceSnapshot deviceSnapshot = mBugContext.getDeviceSnapshot();
        reportParams.put("operating_system_version", deviceSnapshot.getOSVersion());
        reportParams.put("device_manufacturer", deviceSnapshot.getDeviceManufacturer());
        reportParams.put("device_model", deviceSnapshot.getDeviceModel());
        reportParams.put("device_brand", deviceSnapshot.getDeviceBrand());
        reportParams.put("device_identifier", deviceSnapshot.getDeviceIdentifier());

        EnvironmentSnapshot environmentSnapshot = mBugContext.getEnvironmentSnapshot();
        reportParams.put("total_capacity_bytes", environmentSnapshot.getTotalCapacityBytes());
        reportParams.put("free_capacity_bytes", environmentSnapshot.getFreeCapacityBytes());
        reportParams.put("free_memory_bytes", environmentSnapshot.getFreeMemoryBytes());
        reportParams.put("total_memory_bytes", environmentSnapshot.getTotalMemoryBytes());
        reportParams.put("battery_level", environmentSnapshot.getBatteryLevel());
        reportParams.put("carrier_name", environmentSnapshot.getCarrierName());
        reportParams.put("android_mobile_network_subtype", environmentSnapshot.getMobileNetworkSubtype());
        reportParams.put("wifi_connected", environmentSnapshot.getWifiConnected());
        reportParams.put("locale", environmentSnapshot.getLocale());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");

        reportParams.put("invoked_at", sdf.format(environmentSnapshot.getInvokedAt()));
        reportParams.put("submission_attempts", 1);

        // Attachments
        JSONArray attachmentsParams = new JSONArray();

        for (Attachment attachment : mBugContext.getAttachments()) {
            // TODO: Handle these JSON exceptions separately? So that bug reports can still be submitted
            attachmentsParams.put(attachment.getJSONObject());
        }

        if (attachmentsParams.length() > 0) {
            reportParams.put("attachments", attachmentsParams);
        }

        // Attributes
        JSONObject attributesParams = new JSONObject();
        AttributeMap attributes = mBugContext.getAttributes();

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            String attributeName = attribute.getKey();

            if (attributeName.equals(TextInputField.SUMMARY_ATTRIBUTE_NAME)) {
                // Skip system attributes
                continue;
            }

            JSONObject attributeParams = new JSONObject();
            attributeParams.put("attribute_type", 0);
            attributeParams.put("attribute_value", attribute.getValue());
            attributesParams.put(attributeName, attributeParams);
        }

        if (attributesParams.length() > 0) {
            reportParams.put("attributes", attributesParams);
        }

        appParams.put("bundle_short_version", sessionSnapshot.getBundleShortVersion());
        appParams.put("bundle_version", sessionSnapshot.getBundleVersion());
        appParams.put("bundle_identifier", sessionSnapshot.getBundleIdentifier());
        appParams.put("bundle_name", sessionSnapshot.getBundleName());
        appParams.put("platform", sessionSnapshot.getPlatform());

        params.put("report", reportParams);
        params.put("app", appParams);

        ApiIdentity identity = mBugContext.getApiIdentity();
        if (identity instanceof ApiIdentity.ApiKey) {
            params.put("api_key", identity.getId());
        } else if (identity instanceof ApiIdentity.EmailAddress) {
            params.put("email", identity.getId());
        }

        return params;
    }
}
