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
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
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
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.buglife.sdk.reporting.BugReporter;
import com.buglife.sdk.reporting.SubmitReportService;
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
    private final BugReporter reporter;

    private Client(Application application, BugReporter reporter, @Nullable String apiKey, @Nullable String email) {
        mAppContext = application.getApplicationContext();
        this.reporter = reporter;
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
            // Try to start the screenshot observer. However, if permission is denied by the user,
            // then disable screenshot invocations. This way, the user doesn't repeatedly get prompted to
            // grant permissions, but screenshot invocations can still be re-enabled programattically in the same session.
            mScreenshotObserver.start(foregroundActivity, new ScreenshotObserver.ScreenshotObserverPermissionListener() {
                @Override
                public void onPermissionDenied() {
                    if (getInvocationMethod() == InvocationMethod.SCREENSHOT) {
                        setInvocationMethod(InvocationMethod.NONE);
                    }
                }
            });
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
        Activity activity = mForegroundDetector.getCurrentActivity();
        final AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.buglife_alert_dialog).create();
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
        BugContext.Builder builder = new BugContext.Builder(mAppContext)
                .setUserEmail(mUserEmail)
                .setUserIdentifier(mUserIdentifier)
                .setApiKey(mApiKey)
                .setApiEmail(mEmail);

        if (mListener != null) {
            mListener.onAttachmentRequest();
        }

        builder.setAttachments(mQueuedAttachments);
        mQueuedAttachments.clear();

        builder.setAttributes(mAttributes);

        return builder.build();
    }

    void submitReport(Report report, RequestHandler requestHandler) {
        reporter.report(report);
    }

    /**
     * Called on successful report submissions, as well as cancellation.
     */
    void onFinishReportFlow() {
        mReportFlowVisible = false;
        AttachmentDataCache.getInstance().clear();
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
            return new Client(mApplication, new BugReporterImpl(mApplication), apiKey, null);
        }

        Client buildWithEmail(String email) {
            return new Client(mApplication, new BugReporterImpl(mApplication), null, email);
        }
    }

    private static Attachment.Builder getScreenshotBuilder() {
        return new Attachment.Builder(DEFAULT_SCREENSHOT_FILENAME, DEFAULT_SCREENSHOT_ATTACHMENT_TYPE);
    }
}
