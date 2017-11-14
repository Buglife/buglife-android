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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.buglife.sdk.reporting.BugReporter;
import com.buglife.sdk.screenrecorder.ScreenRecorder;
import com.buglife.sdk.screenrecorder.ScreenRecordingPermissionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_ATTACHMENT;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_BUG_CONTEXT;

final class Client implements ForegroundDetector.OnForegroundListener, InvocationMethodManager.OnInvocationMethodTriggeredListener {
    private static final InvocationMethod DEFAULT_INVOCATION_METHOD = InvocationMethod.SHAKE;
    private static final String PERMISSION_INTERNET = "android.permission.INTERNET";
    private static final String PERMISSION_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String PERMISSION_SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
    private static final String PERMISSION_ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
    private static final String DEFAULT_SCREENSHOT_FILENAME = "Screenshot.jpg";
    private static final String DEFAULT_SCREENSHOT_ATTACHMENT_TYPE = Attachment.TYPE_PNG;

    @NonNull private final Context mAppContext;
    private final ApiIdentity mApiIdentity;
    @Nullable private BuglifeListener mListener;
    @NonNull private InvocationMethod mInvocationMethod;
    @Nullable private InvocationMethodManager mInvocationMethodManager;
    private final ForegroundDetector mForegroundDetector;
    @Nullable private String mUserIdentifier = null;
    @Nullable private String mUserEmail = null;
    @NonNull private final ArrayList<Attachment> mQueuedAttachments;
    @NonNull private final AttributeMap mAttributes;
    @Nullable private ArrayList<InputField> mInputFields;
    private boolean mReportFlowVisible = false;
    private final BugReporter reporter;

    Client(Application application, BugReporter reporter,  ApiIdentity apiIdentity) {
        mAppContext = application.getApplicationContext();
        this.reporter = reporter;
        mApiIdentity = apiIdentity;
        mQueuedAttachments = new ArrayList();
        mAttributes = new AttributeMap();
        mForegroundDetector = new ForegroundDetector(application, this);

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
        if (mForegroundDetector.getForegrounded()) {
            startInvocationMethod();
        }
    }

    @Override
    public void onForegroundEvent() {
        startInvocationMethod();
    }

    @Override
    public void onBackgroundEvent() {
        stopInvocationMethod();
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

    Bitmap getScreenshot() {
        Screenshotter screenshotter = new Screenshotter(mForegroundDetector.getCurrentActivity());
        return screenshotter.getBitmap();
    }

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

    void startScreenRecording() {
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
                .setApiIdentity(mApiIdentity);

        if (mListener != null) {
            mListener.onAttachmentRequest();
        }

        builder.setAttachments(mQueuedAttachments);
        mQueuedAttachments.clear();

        builder.setAttributes(mAttributes);

        return builder.build();
    }

    void submitReport(Report report) {
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

    @Override public void onShakeInvocationMethodTriggered() {
        if (mReportFlowVisible) {
            return;
        }

        if (mInvocationMethod == InvocationMethod.SHAKE) {
            Bitmap bitmap = getScreenshot();
            onScreenshotTaken(bitmap);
        }
    }

    @Override public void onScreenshotInvocationMethodTriggered(File file) {
        onScreenshotTakenFromBackgroundThread(file);
    }

    /***************************
     * BUILDER
     ***************************/

    static class Builder {
        private Application mApplication;

        Builder(Application application) {
            mApplication = application;
        }

        Client buildWithApiKey(String apiKey) {
            return new Client(mApplication, new BugReporterImpl(mApplication), new ApiIdentity.ApiKey(apiKey));
        }

        Client buildWithEmail(String email) {
            return new Client(mApplication, new BugReporterImpl(mApplication), new ApiIdentity.EmailAddress(email));
        }
    }

    private static Attachment.Builder getScreenshotBuilder() {
        return new Attachment.Builder(DEFAULT_SCREENSHOT_FILENAME, DEFAULT_SCREENSHOT_ATTACHMENT_TYPE);
    }

    private void startInvocationMethod() {
        if (mInvocationMethodManager == null) {
            Activity activity = mForegroundDetector.getCurrentActivity();
            mInvocationMethodManager = new InvocationMethodManager(activity, this);
        }
        mInvocationMethodManager.start(mInvocationMethod);
    }

    private void stopInvocationMethod() {
        if (mInvocationMethodManager != null) {
            mInvocationMethodManager.stop();
            mInvocationMethodManager = null;
        }
    }
}
