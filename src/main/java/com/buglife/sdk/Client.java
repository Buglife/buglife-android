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
import android.app.FragmentManager;
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
import com.buglife.sdk.screenrecorder.ScreenRecorder;
import com.buglife.sdk.screenrecorder.ScreenRecordingPermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_ATTACHMENT;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_BUG_CONTEXT;

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
    private static final long SUBMIT_PENDING_REPORTS_DELAY = 2 * 1000;

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
    @NonNull private final ArrayList<Attachment> mQueuedAttachments;
    @NonNull private final AttributeMap mAttributes;
    @Nullable private ArrayList<InputField> mInputFields;
    private boolean mReportFlowVisible = false;
    @NonNull private final ColorPalette mColorPallete;
    private File mReportsDir;

    private Client(Application application, @Nullable String apiKey, @Nullable String email) {
        mAppContext = application.getApplicationContext();
        mApiKey = apiKey;
        mEmail = email;
        mQueuedAttachments = new ArrayList();
        mAttributes = new AttributeMap();
        mForegroundDetector = new ForegroundDetector(application, this);
        mColorPallete = new ColorPalette.Builder(mAppContext).build();
        mReportsDir = new File(mAppContext.getExternalCacheDir(), "Buglife Reports");

        boolean hasPermissions = checkPermissions();

        if (!hasPermissions) {
            Log.e("Android Manifest missing required permissions");
            throw new Buglife.BuglifeException("Error starting Buglife: Your AndroidManifest.xml is missing one or more permissions");
        }

        setInvocationMethod(DEFAULT_INVOCATION_METHOD);

        // Wait a few seconds before submitting pending reports. This ensures that the host application
        // can kick off & prioritize more critical tasks immediately upon launch.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                submitPendingReports();
                clearPreviouslySavedRecordings();
            }
        }, SUBMIT_PENDING_REPORTS_DELAY);
    }

    // If the user kills the app or it crashes during the report activity,
    // then there's nothing sensible to be done with the recordings except to delete them.
    // It's not polite to waste the user's disk space.
    private void clearPreviouslySavedRecordings() {
        File recordingsFolder = new File(mAppContext.getExternalCacheDir(), "Buglife");
        if (recordingsFolder.exists() && recordingsFolder.isDirectory()) {
            File ls[] = recordingsFolder.listFiles();
            for (File f: ls) {
                f.delete();
            }
        }
    }
    private void submitPendingReports() {
        HashMap<File, JSONObject> pendingReports = pendingReports();
        if (pendingReports.isEmpty()) {
            return;
        }
        File files[] = new File[pendingReports.size()];
        pendingReports.keySet().toArray(files);
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                long lhslm = lhs.lastModified();
                long rhslm = rhs.lastModified();
                return (int)(rhslm-lhslm);
            }
        });
        for (final File reportFile: files) {
            final JSONObject pendingReport = pendingReports.get(reportFile);
            try {
                int submissionCount = pendingReport.getInt("submission_attempts");
                submissionCount++;
                pendingReport.put("submission_attempts", submissionCount);
            } catch (JSONException e) {
                Log.e("Failed to get or set submission count from previously saved report");
                // this is not a fatal error... probably
            }
            makeJsonObjectRequest(pendingReport, new RequestHandler() {
                @Override
                public void onSuccess() {
                    Log.d("Successfully uploaded previously saved report: " + reportFile.getName());
                    reportFile.delete();
                }

                @Override
                public void onFailure(Throwable e) {
                    Log.e("Failed to upload previously saved report: " + e.toString());
                    savePendingReport(pendingReport);
                }
            });
        }
    }

    private void savePendingReport(JSONObject pendingReport) {
        if (!mReportsDir.exists()) {
            if (!mReportsDir.mkdirs())
            {
                Log.e("Unable to create \"Buglife Reports\" directory");
                return;
            }
        }
        if (!mReportsDir.isDirectory()) {
            Log.e("Someone has created a file named \"Buglife Reports\" here already.");
            return;
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(mReportsDir, pendingReport.getJSONObject("report").getString("invoked_at") + ".json"));
            OutputStreamWriter osw = new OutputStreamWriter(outputStream);
            osw.write(pendingReport.toString());
            osw.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private HashMap<File, JSONObject> pendingReports() {
        HashMap<File, JSONObject> pendingReports = new HashMap<File, JSONObject>();
        File pendingReportFiles[] = mReportsDir.listFiles();
        if (pendingReportFiles == null) {
            return pendingReports;
        }
        for (File reportFile: pendingReportFiles) {
            StringBuffer jsonContent = new StringBuffer();
            try {
                FileInputStream jsonInputStream = new FileInputStream(reportFile);
                byte[] buffer = new byte[1024];
                int n = 0;
                while ((n = jsonInputStream.read(buffer)) != -1) {
                    jsonContent.append(new String(buffer, 0, n));
                }
                jsonInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Failed to read pending report file, abandoning");
            }
            try {
                JSONObject report = new JSONObject(jsonContent.toString());
                pendingReports.put(reportFile, report);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Failed to convert report to JSON object, abandoning");
            }
        }
        return pendingReports;
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

    void putAttribute(@NonNull String key, @Nullable String value) {
        mAttributes.put(key, value);
    }

    void setInputFields(@NonNull InputField... inputFields) {
        mInputFields = new ArrayList(Arrays.asList(inputFields));
    }

    List<InputField> getInputFields() {
        ArrayList<InputField> inputFields = mInputFields;

        if (inputFields == null || inputFields.isEmpty()) {
            TextInputField summaryInputField = TextInputField.summaryInputField();
            inputFields = new ArrayList();
            inputFields.add(summaryInputField);
        }

        // return a copy of the array so that adding new fields during
        // a report flow doesn't break things
        return new ArrayList(inputFields);
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
        showAlertDialog(screenshotAttachment);
    }

    private void onScreenshotTaken(File screenshotFile) {
        if (!canInvokeBugReporter()) {
            return;
        }

        Attachment screenshotAttachment = getScreenshotBuilder().build(screenshotFile);
        showAlertDialog(screenshotAttachment);
    }

    private boolean canInvokeBugReporter() {
        return !mReportFlowVisible && mForegroundDetector.getForegrounded();
    }

    private void showAlertDialog(@NonNull final Attachment screenshotAttachment) {
        mReportFlowVisible = true;
        final AlertDialog alertDialog = new AlertDialog.Builder(mAppContext, R.style.buglife_alert_dialog).create();
        alertDialog.setTitle(R.string.help_us_make_this_app_better);
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mAppContext.getString(R.string.report_a_bug), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Show the reporter flow starting with the screenshot annotator
                startBuglifeActivity(ScreenshotAnnotatorActivity.class, screenshotAttachment);
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
        // showReporter() can be called manually, so check to make sure it isn't already visible
        if (mReportFlowVisible) {
            Log.e("Unable to show reporter; Buglife is already visible. Did you call showReporter() twice?");
            return;
        }

        startBuglifeActivity(ReportActivity.class, null);
    }

    public void startScreenRecording() {
        startScreenRecordingFlow();
    }

    private void startScreenRecordingFlow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(getApplicationContext(), R.string.screen_recording_minimum_os_error, Toast.LENGTH_LONG).show();
            return;
        }

        Activity currentActivity = mForegroundDetector.getCurrentActivity();
        FragmentManager fragmentManager = currentActivity.getFragmentManager();
        ScreenRecordingPermissionHelper permissionHelper = (ScreenRecordingPermissionHelper) fragmentManager.findFragmentByTag(ScreenRecordingPermissionHelper.TAG);

        if (permissionHelper == null) {
            permissionHelper = ScreenRecordingPermissionHelper.newInstance();
            permissionHelper.setPermissionCallback(new ScreenRecordingPermissionHelper.PermissionCallback() {
                @Override
                public void onPermissionGranted(int resultCode, Intent data) {
                    startScreenRecordingFlow(resultCode, data);
                }

                @Override
                public void onPermissionDenied(ScreenRecordingPermissionHelper.PermissionType permissionType) {
                    int toastStringResId = 0;

                    switch (permissionType) {
                        case OVERLAY:
                            toastStringResId = R.string.screen_recording_permission_denied_overlay;
                            break;
                        case RECORDING:
                            toastStringResId = R.string.screen_recording_permission_denied_recording;
                            break;
                    }

                    if (toastStringResId != 0) {
                        Toast.makeText(getApplicationContext(), toastStringResId, Toast.LENGTH_LONG).show();
                    }
                }
            });
            fragmentManager.beginTransaction().add(permissionHelper, PermissionHelper.TAG).commit();
        }
    }

    private void startScreenRecordingFlow(int resultCode, Intent data) {
        ScreenRecorder screenRecorder = new ScreenRecorder(getApplicationContext(), resultCode, data);
        screenRecorder.start();
    }

    private void startBuglifeActivity(Class cls, @Nullable Attachment screenshotAttachment) {
        BugContext bugContext = buildBugContext();

        mReportFlowVisible = true;
        Intent intent = new Intent(mAppContext, cls);
        intent.setFlags(intent.getFlags() | FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(INTENT_KEY_BUG_CONTEXT, bugContext);

        if (screenshotAttachment != null) {
            intent.putExtra(INTENT_KEY_ATTACHMENT, screenshotAttachment);
        }

        mAppContext.startActivity(intent);
    }

    private @NonNull BugContext buildBugContext() {
        BugContext.Builder builder = new BugContext.Builder(mAppContext);

        if (mListener != null) {
            mListener.onAttachmentRequest();
        }

        builder.setAttachments(mQueuedAttachments);
        mQueuedAttachments.clear();

        builder.setAttributes(mAttributes);

        return builder.build();
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
        AttachmentDataCache.getInstance().clear();
    }

    private JSONObject getReportParams(Report report) throws JSONException {
        String whatHappened = report.getBugContext().getAttribute(TextInputField.SUMMARY_ATTRIBUTE_NAME);
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");

        reportParams.put("invoked_at", sdf.format(environmentSnapshot.getInvokedAt()));
        reportParams.put("submission_attempts", 1);

        // Attachments
        JSONArray attachmentsParams = new JSONArray();

        for (Attachment attachment : report.getBugContext().getAttachments()) {
            // TODO: Handle these JSON exceptions separately? So that bug reports can still be submitted
            attachmentsParams.put(attachment.getJSONObject());
        }

        if (attachmentsParams.length() > 0) {
            reportParams.put("attachments", attachmentsParams);
        }

        // Attributes
        JSONObject attributesParams = new JSONObject();
        AttributeMap attributes = report.getBugContext().getAttributes();

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

    private void makeJsonObjectRequest(final JSONObject parameters, final RequestHandler requestHandler) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, BUGLIFE_URL, parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                requestHandler.onSuccess();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                savePendingReport(parameters);
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
