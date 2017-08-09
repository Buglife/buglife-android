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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;

public final class PermissionHelper extends Fragment {
    private static final int REQUEST_PERMISSIONS = 10;
    public static final String TAG = "com.buglife.PermissionHelper";

    private PermissionCallback mCallback;
    private static boolean sReadExternalStoragePermissionDenied;

    public static PermissionHelper newInstance() {
        return new PermissionHelper();
    }

    public PermissionHelper() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure the fragment isn't destroyed & recreated on orientation changes
        setRetainInstance(true);
    }

    // This should be called *before* attaching the fragment
    void setPermissionCallback(PermissionCallback callback) {
        mCallback = callback;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        checkPermissions();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (mCallback == null) {
            return;
        }

        Activity activity = getActivity();

        if (activity == null) {
            Log.e("Error: Couldn't get activity for PermissionHelper fragment");
            return;
        }

        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

        if (ActivityUtils.arePermissionsGranted(activity, permissions)) {
            mCallback.onPermissionGranted();
        } else {
            if (sReadExternalStoragePermissionDenied) {
                mCallback.onPermissionDenied();
            } else {
                requestPermissions(permissions, REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (verifyPermissions(grantResults)) {
                mCallback.onPermissionGranted();
            } else {
                mCallback.onPermissionDenied();
            }

            mCallback = null;
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    private static boolean verifyPermissions(int[] grantResults) {
        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
