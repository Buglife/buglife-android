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

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.List;

/**
 * Buglife! Handles initialization and configuration of Buglife.
 */
public final class Buglife {
    @Nullable private static Client mClient = null;

    private Buglife() {}

    /**
     * Enables Buglife bug reporting within your app. This method should be called from
     * the onCreate() method in your Application subclass. If your project doesn't already
     * have a custom Application subclass, you'll need to create one.
     *
     * @param application Your application subclass, usually <code>this</code>
     * @param apiKey      Your Buglife API key. You can find this on the Buglife web dashboard
     */
    public static void initWithApiKey(@NonNull Application application, @NonNull String apiKey) {
        verifyDependencies();
        mClient = new Client.Builder(application).buildWithApiKey(apiKey);
    }

    /**
     * Call this method with your own email address if you'd like to try out Buglife without signing
     * up for an account. Bug reports will be sent directly to the provided email.
     *
     * This method should be called from
     * the onCreate() method in your Application subclass. If your project doesn't already
     * have a custom Application subclass, you'll need to create one.
     *
     * @param application Your application subclass, usually <code>this</code>
     * @param email       The email address to which bug reports should be sent. This email address
     *                    should belong to you or someone on your team.
     */
    public static void initWithEmail(@NonNull Application application, @NonNull String email) {
        verifyDependencies();
        mClient = new Client.Builder(application).buildWithEmail(email);
    }

    /**
     * Specifies the method for which users initiate the Buglife bug report flow.
     *
     * @param invocationMethod The invocation method
     */
    public static void setInvocationMethod(@NonNull InvocationMethod invocationMethod) {
        getClient().setInvocationMethod(invocationMethod);
    }

    public static InvocationMethod getInvocationMethod() {
        return getClient().getInvocationMethod();
    }

    /**
     * Specifies a user identifier that will be submitted with bug reports. This can be a name,
     * database identifier, or any other string of your choosing.
     *
     * @note The userIdentifier is *not* persisted across app launches.
     *
     * @param userIdentifier The user identifier.
     */
    public static void setUserIdentifier(@Nullable String userIdentifier) {
        getClient().setUserIdentifier(userIdentifier);
    }

    /**
     * Specifies an email for the user that will be submitted with bug reports.
     *
     * @note This is the email address of the user of your app; This is NOT the email address
     *       to which bug reports will be sent.
     *
     * @param userEmail The user email.
     */
    public static void setUserEmail(@Nullable String userEmail) {
        getClient().setUserEmail(userEmail);
    }

    /**
     * Captures a screenshot of the current activity.
     * Deprecated, use {@link Buglife#captureScreenshot()} instead.
     *
     * @return A bitmap of the generated screenshot
     */
    @Deprecated
    public static Bitmap getScreenshotBitmap() {
        return getClient().getScreenshot();
    }

    /**
     * Captures a screenshot of the current activity.
     *
     * @return A {@link java.io.File} that represents the screenshot. If screenshot failed, this
     * will return null.
     */
    @Nullable
    public static FileAttachment captureScreenshot() {
        return getClient().captureScreenshot();
    }

    /**
     * Queues an attachment for the next bug report draft.
     * Deprecated, use {@link Buglife#addAttachment(FileAttachment)} instead.
     *
     * @param attachment The attachment
     */
    @Deprecated
    public static void addAttachment(@NonNull Attachment attachment) {
        getClient().addAttachment(attachment);
    }

    /**
     * Queues an attachment for the next bug report draft.
     *
     * @param attachment The attachment
     */
    public static void addAttachment(@NonNull FileAttachment attachment) {
        getClient().addAttachment(attachment);
    }

    /**
     * Adds custom data to bug reports. Set a `null` value for a given attribute to delete its
     * current value.
     *
     * Custom attributes are not automatically cleared after a bug report is submitted.
     *
     * @param key The attribute key
     * @param value The attribute value
     */
    public static void putAttribute(@NonNull String key, @Nullable String value) {
        getClient().putAttribute(key, value);
    }

    /**
     * Returns the input fields to the shown in the bug reporter UI.
     */
    static @NonNull List<InputField> getInputFields() {
        return getClient().getInputFields();
    }

    /**
     * Configures the input fields to be shown in the bug reporter UI.
     * @param inputFields The input fields
     */
    public static void setInputFields(@NonNull InputField... inputFields) {
        getClient().setInputFields(inputFields);
    }

    /**
     * Sets a listener for Buglife-related callbacks.
     *
     * @param listener The listener
     */
    public static void setListener(@Nullable BuglifeListener listener) {
        getClient().setListener(listener);
    }

    /**
     * Manually presents the bug reporter UI.
     */
    public static void showReporter() {
        getClient().showReporter();
    }

    /**
     * Manually starts the screen recording flow; users can record up to 30 seconds, or tap
     * the record button to stop recording early. Once screen recording has been
     * stopped, the bug reporter UI will be shown with the screen recording attached.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static void startScreenRecording() {
        getClient().startScreenRecording();
    }

    static void submitReport(Report report) {
        getClient().submitReport(report);
    }

    static void onFinishReportFlow() {
        getClient().onFinishReportFlow();
    }

    @Deprecated
    static Context getContext() {
        return getClient().getContext();
    }

    private static void verifyDependencies() {
        try {
            Class<?> clazz = Class.forName("com.android.volley.toolbox.JsonObjectRequest");
        } catch (ClassNotFoundException e) {
            throw new BuglifeException("Unable to initialize Buglife. Dependency `com.android.volley:volley` not found");
        }

        try {
            Class<?> clazz = Class.forName("android.support.v7.app.ActionBar");
        } catch (ClassNotFoundException e) {
            throw new BuglifeException("Unable to initialize Buglife. Dependency `com.android.support:appcompat-v7` not found");
        }
    }

    private static Client getClient() {
        if (mClient == null) {
            // TODO: Change this to a toast + log?
            throw new BuglifeException("Buglife not initialized. Double-check that you are (1) calling Buglife.initWithApiKey() or Buglife.initWithEmail() from your Application subclass, and (2) your Application subclass is declared in AndroidManifest.xml");
        }

        return mClient;
    }

    public static class BuglifeException extends RuntimeException {
        public BuglifeException(String message) {
            super(message);
        }
    }
}
