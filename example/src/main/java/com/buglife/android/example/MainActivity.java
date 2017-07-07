package com.buglife.android.example;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.buglife.sdk.Buglife;
import com.buglife.sdk.InvocationMethod;
import com.buglife.sdk.screenrecorder.ScreenRecorder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView introTextView = (TextView) findViewById(R.id.intro_text_view);
        introTextView.setText(getIntroText());
    }

    private ScreenRecorder mScreenRecorder;
    private static final int SCREEN_OVERLAY_REQUEST_CODE = 1234;
    private static final int SCREEN_RECORD_REQUEST_CODE = 12345;

    void reportBugButtonTapped(View view) {
//        Bitmap screenshot = Buglife.getScreenshotBitmap();
//        Attachment attachment = new Attachment.Builder("Screenshot.png", TYPE_PNG).build(screenshot);
//        Buglife.addAttachment(attachment);
//
//
//        Bitmap walter = BitmapFactory.decodeResource(getResources(), R.drawable.walter);
//        Attachment attachment1 = new Attachment.Builder("walter.jpg", Attachment.TYPE_JPEG).build(walter);
//        Buglife.addAttachment(attachment1);
//
//        Buglife.showReporter();

        startScreenRecording();
    }

    private void startScreenRecording() {
        // First make sure we can draw an overlay
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, SCREEN_OVERLAY_REQUEST_CODE);
        } else {
            onOverlayPermissionsGranted();
        }
    }

    private void onOverlayPermissionsGranted() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, SCREEN_RECORD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_OVERLAY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                onOverlayPermissionsGranted();
            } else {
                Toast.makeText(this, "Screen overlays must be enabled to perform screen recording", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mScreenRecorder = new ScreenRecorder(this, resultCode, data);
                mScreenRecorder.start();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getIntroText() {
        if (isEmulator()) {
            return "Reporting bugs via screenshot / shake requires running on a device. Or you can manually report a bug by tapping the button below.";
        } else {
            InvocationMethod invocationMethod = Buglife.getInvocationMethod();

            switch (invocationMethod) {
                case SCREENSHOT:
                    return "Take a screenshot to report a bug! Or tap the button below.";
                case SHAKE:
                    return "Shake your device to report a bug! Or tap the button below.";
                default:
                    return "Tap the button below to report a bug.";
            }
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
