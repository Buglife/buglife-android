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

import android.location.Location;

import com.buglife.sdk.reporting.DeviceSnapshot;
import com.buglife.sdk.reporting.EnvironmentSnapshot;
import com.buglife.sdk.reporting.SessionSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a bug report draft.
 */
public final class Report {
    private final BugContext mBugContext;

    Report(BugContext bugContext) {
        mBugContext = bugContext;
    }

    public JSONObject toJSON() throws JSONException, IOException {
        JSONObject params = new JSONObject();
        JSONObject reportParams = new JSONObject();
        JSONObject appParams = new JSONObject();

        Attribute summaryAttribute = mBugContext.getAttribute(TextInputField.SUMMARY_ATTRIBUTE_NAME);

        if (summaryAttribute != null) {
            reportParams.put("what_happened", summaryAttribute.getValue());
        }

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
        reportParams.put("invocation_method", environmentSnapshot.getInvokationMethod().getValue());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ", Locale.US);

        reportParams.put("invoked_at", sdf.format(environmentSnapshot.getInvokedAt()));
        reportParams.put("submission_attempts", 1);

        // Attachments
        JSONArray attachmentsParams = new JSONArray();

        for (FileAttachment attachment : mBugContext.getAttachments()) {
            // TODO: Handle these JSON exceptions separately? So that bug reports can still be submitted
            attachmentsParams.put(attachment.toJSON());
            attachment.getFile().delete();
        }

        if (attachmentsParams.length() > 0) {
            reportParams.put("attachments", attachmentsParams);
        }

        // Attributes
        JSONObject attributesParams = new JSONObject();
        AttributeMap attributes = mBugContext.getAttributes();

        //well this is a little awkward
        Location location = environmentSnapshot.getLocation();

        if (location != null) {
            Attribute deviceLocation = new Attribute("" + location.getLatitude() + "," + location.getLongitude(), Attribute.ValueType.STRING, Attribute.FLAG_SYSTEM);
            attributes.put("Device location", deviceLocation);
        }

        for (Map.Entry<String, Attribute> attribute : attributes.entrySet()) {
            String attributeName = attribute.getKey();
            Attribute attr = attribute.getValue();

            if (attributeName.equals(TextInputField.SUMMARY_ATTRIBUTE_NAME)) {
                // Skip system attributes
                continue;
            }

            JSONObject attributeParams = new JSONObject();
            attributeParams.put("attribute_type", attr.getValueType());
            attributeParams.put("attribute_value", attr.getValue());
            attributeParams.put("flag", attr.getFlags());
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
