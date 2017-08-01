package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.widget.Toast;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

/**
 * Manages verification of & requesting of the permissions necessary to
 * begin screen recording.
 */
@TargetApi(Build.VERSION_CODES.M)
public final class ScreenRecordingPermissionHelper extends Fragment {

    public static final String TAG = "com.buglife.ScreenRecordingPermissionHelper";
    private static final int SCREEN_OVERLAY_REQUEST_CODE = 1234;
    private static final int SCREEN_RECORD_REQUEST_CODE = 12345;

    private PermissionCallback mPermissionCallback;

    public static ScreenRecordingPermissionHelper newInstance() {
        return new ScreenRecordingPermissionHelper();
    }

    public ScreenRecordingPermissionHelper() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure the fragment isn't destroyed & recreated on orientation changes
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        checkOverlayPermissions();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setPermissionCallback(PermissionCallback permissionCallback) {
        mPermissionCallback = permissionCallback;
    }

    private void checkOverlayPermissions() {
        // First make sure we can draw an overlay
        if (!Settings.canDrawOverlays(getContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
            startActivityForResult(intent, SCREEN_OVERLAY_REQUEST_CODE);
        } else {
            onOverlayPermissionsGranted();
        }
    }

    private void onOverlayPermissionsGranted() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, SCREEN_RECORD_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_OVERLAY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onOverlayPermissionsGranted();
            } else {
                mPermissionCallback.onPermissionDenied(PermissionType.OVERLAY);
            }
        } else if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mPermissionCallback.onPermissionGranted(resultCode, data);
            } else {
                mPermissionCallback.onPermissionDenied(PermissionType.RECORDING);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public enum PermissionType {
        OVERLAY, RECORDING
    }

    public interface PermissionCallback {
        void onPermissionGranted(int resultCode, Intent data);
        void onPermissionDenied(PermissionType permissionType);
    }
}
