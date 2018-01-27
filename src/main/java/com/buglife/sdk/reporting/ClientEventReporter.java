/*
 * Copyright (C) 2018 Buglife, Inc.
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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.buglife.sdk.ApiIdentity;
import com.buglife.sdk.Buglife;
import com.buglife.sdk.Log;
import com.buglife.sdk.NetworkManager;

import org.json.JSONException;
import org.json.JSONObject;



public final class ClientEventReporter {
    private static ClientEventReporter sInstance;
    private final Context mContext;
    private final String BUGLIFE_CLIENT_EVENTS_URL = NetworkManager.BUGLIFE_URL+"/api/v1/client_events.json";
    ClientEventReporter(Context context) {
        mContext = context;
    }
    public static synchronized ClientEventReporter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ClientEventReporter(context);
        }

        return sInstance;

    }

    public void reportClientEvent(final String eventName, ApiIdentity identity)
    {
        JSONObject params = new JSONObject();
        DeviceSnapshot deviceSnapshot = new DeviceSnapshot(mContext);
        SessionSnapshot sessionSnapshot = new SessionSnapshot(mContext, Buglife.getUserEmail(), Buglife.getUserIdentifier());
        try {
            JSONObject clientEventParams = new JSONObject();
            if (deviceSnapshot.getDeviceIdentifier() != null) {
                clientEventParams.put("device_identifier", deviceSnapshot.getDeviceIdentifier());
            }
            clientEventParams.put("sdk_version", sessionSnapshot.getSDKVersion());
            clientEventParams.put("sdk_name", sessionSnapshot.getSDKName());
            clientEventParams.put("event_name", eventName);
            clientEventParams.put("bundle_short_version", sessionSnapshot.getBundleShortVersion());
            clientEventParams.put("bundle_version", sessionSnapshot.getBundleVersion());
            if (sessionSnapshot.getUserEmail() != null) {
                clientEventParams.put("user_email", sessionSnapshot.getUserEmail());
            }
            if (sessionSnapshot.getUserIdentifier() != null) {
                clientEventParams.put("user_identifier", sessionSnapshot.getUserIdentifier());
            }

            JSONObject appParams = new JSONObject();
            appParams.put("bundle_identifier", sessionSnapshot.getBundleIdentifier());
            appParams.put("platform", sessionSnapshot.getPlatform());
            appParams.put("bundle_name", sessionSnapshot.getBundleName());
            params.put("app", appParams);
            params.put("client_event", clientEventParams);
            String key = identity instanceof ApiIdentity.EmailAddress ? "email" : "api_key";
            params.put(key, identity.getId());

        }
        catch (JSONException e) {
            //figure out what to do here
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BUGLIFE_CLIENT_EVENTS_URL, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Client Event posted successfully: " + eventName);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error submitting client event", error);
            }
        });
        NetworkManager.getInstance(mContext).addToRequestQueue(request);

    }
}
