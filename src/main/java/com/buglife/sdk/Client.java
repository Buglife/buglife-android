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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

final class Client implements ForegroundDetector.OnForegroundListener {
    private static final InvocationMethod DEFAULT_INVOCATION_METHOD = InvocationMethod.SHAKE;
    private static final String SDK_NAME = "Buglife Android";
    private static final String PLATFORM = "android";
    private static final String BUGLIFE_URL = "https://www.buglife.com/api/v1/reports.json";
    private static final String PERMISSION_INTERNET = "android.permission.INTERNET";
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String PERMISSION_SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
    private static final String PERMISSION_ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
    private static final String DEFAULT_SCREENSHOT_FILENAME = "Screenshot.jpg";
    private static final String DEFAULT_SCREENSHOT_ATTACHMENT_TYPE = Attachment.TYPE_PNG;

    @NonNull private final Context mAppContext;
    @Nullable private final String mApiKey;
    @Nullable private final String mEmail;
    @Nullable private BuglifeListener mListener;
    @NonNull private InvocationMethod mInvocationMethod;
    @Nullable private SensorManager mSensorManager = null;
    @Nullable private Sensor mAccelerometer = null;
    @Nullable private ShakeDetector mShakeDetector = null;
    @Nullable private ScreenshotObserver mScreenshotObserver = null;
    private final ForegroundDetector mForegroundDetector;
    @Nullable private String mUserIdentifier = null;
    @Nullable private String mUserEmail = null;
    private final ArrayList<Attachment> mQueuedAttachments;
    private boolean mReportFlowVisible = false;
    @NonNull private final ColorPalette mColorPallete;

    private Client(Application application, @Nullable String apiKey, @Nullable String email) {
        mAppContext = application.getApplicationContext();
        mApiKey = apiKey;
        mEmail = email;
        mQueuedAttachments = new ArrayList<>();
        mForegroundDetector = new ForegroundDetector(application, this);
        mColorPallete = new ColorPalette.Builder(mAppContext).build();

        boolean hasPermissions = checkPermissions();

        if (!hasPermissions) {
            Log.e("Android Manifest missing required permissions");
            throw new Buglife.BuglifeException("Error starting Buglife: Your AndroidManifest.xml is missing one or more permissions");
        }

        setInvocationMethod(DEFAULT_INVOCATION_METHOD);
    }

    private boolean checkPermissions() {
        PackageInfo packageInfo;

        try {
            packageInfo = mAppContext.getPackageManager().getPackageInfo(mAppContext.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Unable to obtain package info", e);
            return false;
        }

        List requestedPermissions = Arrays.asList(packageInfo.requestedPermissions);
        List requiredPermissions = Arrays.asList(PERMISSION_INTERNET, PERMISSION_WRITE_EXTERNAL_STORAGE, PERMISSION_READ_EXTERNAL_STORAGE, PERMISSION_SYSTEM_ALERT_WINDOW, PERMISSION_ACCESS_NETWORK_STATE);
        return requestedPermissions.containsAll(requiredPermissions);
    }

    Context getApplicationContext() {
        return mAppContext;
    }

    ColorPalette getColorPalette() {
        return mColorPallete;
    }

    void setListener(@Nullable BuglifeListener listener) {
        mListener = listener;
    }

    void setUserIdentifier(@Nullable String userIdentifier) {
        mUserIdentifier = userIdentifier;
    }

    void setUserEmail(@Nullable String userEmail) {
        mUserEmail = userEmail;
    }

    void setInvocationMethod(InvocationMethod invocationMethod) {
        mInvocationMethod = invocationMethod;

        setScreenshotInvocationMethodEnabled(invocationMethod == InvocationMethod.SCREENSHOT);
        setShakeInvocationMethodEnabled(invocationMethod == InvocationMethod.SHAKE);

        if (mForegroundDetector.getForegrounded()) {
            Activity foregroundedActivity = mForegroundDetector.getCurrentActivity();
            startListeningForEnabledInvocations(foregroundedActivity);
        }
    }

    private void setScreenshotInvocationMethodEnabled(boolean enabled) {
        if (enabled && mScreenshotObserver == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mScreenshotObserver = new ScreenshotContentObserver(mAppContext, mOnScreenshotTakenListener);
            } else {
                mScreenshotObserver = new ScreenshotFileObserver(mOnScreenshotTakenListener);
            }
        } else if (!enabled && mScreenshotObserver != null) {
            mScreenshotObserver.stop();
            mScreenshotObserver = null;
        }
    }

    private void setShakeInvocationMethodEnabled(boolean enabled) {
        if (enabled && mSensorManager == null) {
            mSensorManager = (SensorManager) mAppContext.getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mShakeDetector = new ShakeDetector(mOnShakeListener);
        } else if (!enabled && mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
            mSensorManager = null;
            mAccelerometer = null;
            mShakeDetector = null;
        }
    }

    private void startListeningForEnabledInvocations(Activity foregroundActivity) {
        if (mSensorManager != null) {
            boolean registered = mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

            if (!registered) {
                Log.e("Unable to register shake listener");
            }
        }

        if (mScreenshotObserver != null) {
            mScreenshotObserver.start(foregroundActivity);
        }
    }

    @Override
    public void onForegroundEvent() {
        Activity currentActivity = mForegroundDetector.getCurrentActivity();
        startListeningForEnabledInvocations(currentActivity);
    }

    @Override
    public void onBackgroundEvent() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }

        if (mScreenshotObserver != null) {
            mScreenshotObserver.stop();
        }
    }

    InvocationMethod getInvocationMethod() {
        return mInvocationMethod;
    }

    void addAttachment(Attachment attachment) {
        mQueuedAttachments.add(attachment);
    }

    private final ShakeDetector.OnShakeListener mOnShakeListener = new ShakeDetector.OnShakeListener() {
        @Override
        public void onShake() {
            if (mReportFlowVisible) {
                return;
            }

            if (mInvocationMethod == InvocationMethod.SHAKE) {
                Bitmap bitmap = getScreenshot();
                onScreenshotTaken(bitmap);
            }
        }
    };

    Bitmap getScreenshot() {
        Screenshotter screenshotter = new Screenshotter(mForegroundDetector.getCurrentActivity());
        return screenshotter.getBitmap();
    }

    private final OnScreenshotTakenListener mOnScreenshotTakenListener = new OnScreenshotTakenListener() {
        @Override
        public void onScreenshotTaken(File file) {
            onScreenshotTakenFromBackgroundThread(file);
        }
    };

    private void onScreenshotTakenFromBackgroundThread(final File file) {
        Handler mainHandler = new Handler(mAppContext.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onScreenshotTaken(file);
            }
        };

        mainHandler.post(runnable);
    }

    private void onScreenshotTaken(Bitmap bitmap) {
        if (!canInvokeBugReporter()) {
            return;
        }

        Attachment screenshotAttachment = getScreenshotBuilder().build(bitmap);
        addAttachment(screenshotAttachment);
        showAlertDialog();
    }

    private void onScreenshotTaken(File screenshotFile) {
        if (!canInvokeBugReporter()) {
            return;
        }

        Attachment screenshotAttachment = null;

        try {
            screenshotAttachment = getScreenshotBuilder().build(screenshotFile);
        } catch (IOException e) {
            Log.e("IOException capturing screenshot: " + screenshotFile, e);
            Toast.makeText(mAppContext, R.string.error_unable_to_read_screenshot, Toast.LENGTH_LONG).show();
            return;
        }

        addAttachment(screenshotAttachment);
        showAlertDialog();
    }

    private boolean canInvokeBugReporter() {
        return !mReportFlowVisible && mForegroundDetector.getForegrounded();
    }

    private void showAlertDialog() {
        mReportFlowVisible = true;
        final AlertDialog alertDialog = new AlertDialog.Builder(mAppContext, R.style.buglife_alert_dialog).create();
        alertDialog.setTitle(R.string.help_us_make_this_app_better);
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mAppContext.getString(R.string.report_a_bug), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showReporter();
                alertDialog.dismiss();
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, mAppContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
                onFinishReportFlow();
            }
        });

        Window alertWindow = alertDialog.getWindow();

        if (alertWindow != null) {
            alertWindow.setType(WindowManager.LayoutParams.TYPE_TOAST);
        }

        alertDialog.show();
    }

    void showReporter() {
        if (mReportFlowVisible) {
            return;
        }

        BugContext.Builder builder = new BugContext.Builder(mAppContext);

        if (mListener != null) {
            mListener.onAttachmentRequest();
        }

        for (Attachment attachment : mQueuedAttachments) {
            builder.addAttachment(attachment);
        }

        BugContext bugContext = builder.build();

        mReportFlowVisible = true;
        Intent intent = new Intent(mAppContext, ReportActivity.class);
        intent.setFlags(intent.getFlags() | FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ReportActivity.INTENT_KEY_BUG_CONTEXT, bugContext);
        mAppContext.startActivity(intent);
    }

    void submitReport(Report report, RequestHandler requestHandler) {
        JSONObject reportParams;

        try {
            reportParams = getReportParams(report);
        } catch (JSONException e) {
            Log.d("Error serializing JSON report", e);
            requestHandler.onFailure(e);
            return;
        }

        makeJsonObjectRequest(reportParams, requestHandler);
    }

    /**
     * Called on successful report submissions, as well as cancellation.
     */
    void onFinishReportFlow() {
        mReportFlowVisible = false;
        mQueuedAttachments.clear();
        AttachmentDataCache.getInstance().clear();
    }

    private JSONObject getReportParams(Report report) throws JSONException {
        String whatHappened = report.getWhatHappened();
        String bundleIdentifier = mAppContext.getPackageName();
        String bundleName = Client.getApplicationName(mAppContext);
        String operatingSystemVersion = android.os.Build.VERSION.RELEASE;
        String deviceManufacturer = Build.MANUFACTURER;
        String deviceModel = Build.MODEL;
        String deviceBrand = Build.BRAND;
        // TOOD: Fix this; the linter warns against using these hardware identifiers :(
        String deviceIdentifier = null; //Settings.Secure.getString(mAppContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        String sdkVersion = com.buglife.sdk.BuildConfig.VERSION_NAME;
        String bundleVersion = null;
        String bundleShortVersion = null;

        try {
            PackageInfo appPackageInfo = mAppContext.getPackageManager().getPackageInfo(bundleIdentifier, 0);
            bundleVersion = Integer.toString(appPackageInfo.versionCode);
            bundleShortVersion = appPackageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Unable to get version information / package information", e);
        }

        JSONObject params = new JSONObject();
        JSONObject reportParams = new JSONObject();
        JSONObject appParams = new JSONObject();

        reportParams.put("sdk_version", sdkVersion);
        reportParams.put("sdk_name", SDK_NAME);
        reportParams.put("what_happened", whatHappened);
        reportParams.put("operating_system_version", operatingSystemVersion);
        reportParams.put("device_manufacturer", deviceManufacturer);
        reportParams.put("device_model", deviceModel);
        reportParams.put("device_brand", deviceBrand);
        reportParams.put("device_identifier", deviceIdentifier);
        reportParams.put("bundle_short_version", bundleShortVersion);
        reportParams.put("bundle_version", bundleVersion);
        reportParams.put("user_email", mUserEmail);
        reportParams.put("user_identifier", mUserIdentifier);

        EnvironmentSnapshot environmentSnapshot = report.getBugContext().getEnvironmentSnapshot();
        reportParams.put("total_capacity_bytes", environmentSnapshot.getTotalCapacityBytes());
        reportParams.put("free_capacity_bytes", environmentSnapshot.getFreeCapacityBytes());
        reportParams.put("free_memory_bytes", environmentSnapshot.getFreeMemoryBytes());
        reportParams.put("total_memory_bytes", environmentSnapshot.getTotalMemoryBytes());
        reportParams.put("battery_level", environmentSnapshot.getBatteryLevel());
        reportParams.put("carrier_name", environmentSnapshot.getCarrierName());
        reportParams.put("android_mobile_network_subtype", environmentSnapshot.getMobileNetworkSubtype());
        reportParams.put("wifi_connected", environmentSnapshot.getWifiConnected());
        reportParams.put("locale", environmentSnapshot.getLocale());

        // Attachments
        JSONArray attachmentsParams = new JSONArray();

        for (Attachment attachment : report.getBugContext().getAttachments()) {
            // TODO: Handle these JSON exceptions separately? So that bug reports can still be submitted
            attachmentsParams.put(attachment.getJSONObject());
        }

        if (attachmentsParams.length() > 0) {
            reportParams.put("attachments", attachmentsParams);
        }

        appParams.put("bundle_short_version", bundleShortVersion);
        appParams.put("bundle_version", bundleVersion);
        appParams.put("bundle_identifier", bundleIdentifier);
        appParams.put("bundle_name", bundleName);
        appParams.put("platform", PLATFORM);

        params.put("report", reportParams);
        params.put("app", appParams);

        if (mApiKey != null) {
            params.put("api_key", mApiKey);
        } else if (mEmail != null) {
            params.put("email", mEmail);
        }

        return params;
    }

    private static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    private boolean isConnectedViaWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager)mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    /***************************
     * NETWORKING
     ***************************/

    private void makeJsonObjectRequest(JSONObject parameters, final RequestHandler requestHandler) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, BUGLIFE_URL, parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                requestHandler.onSuccess();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                requestHandler.onFailure(error);
            }
        });

        NetworkManager.getInstance(mAppContext).addToRequestQueue(jsonObjectRequest);
    }

    /***************************
     * BUILDER
     ***************************/

    static class Builder {
        private Application mApplication;
        @Nullable private String mApiKey;
        @Nullable private String mEmail;

        Builder(Application application) {
            mApplication = application;
        }

        Client buildWithApiKey(String apiKey) {
            return new Client(mApplication, apiKey, null);
        }

        Client buildWithEmail(String email) {
            return new Client(mApplication, null, email);
        }
    }

    private static Attachment.Builder getScreenshotBuilder() {
        return new Attachment.Builder(DEFAULT_SCREENSHOT_FILENAME, DEFAULT_SCREENSHOT_ATTACHMENT_TYPE);
    }
}
