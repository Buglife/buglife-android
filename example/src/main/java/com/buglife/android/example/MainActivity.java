package com.buglife.android.example;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.buglife.sdk.Attachment;
import com.buglife.sdk.Buglife;
import com.buglife.sdk.InvocationMethod;
import com.buglife.sdk.screenrecorder.ScreenRecorder;

import static com.buglife.sdk.Attachment.TYPE_PNG;

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
        Bitmap screenshot = Buglife.getScreenshotBitmap();
        Attachment attachment = new Attachment.Builder("Screenshot.png", TYPE_PNG).build(screenshot);
        Buglife.addAttachment(attachment);


        Bitmap walter = BitmapFactory.decodeResource(getResources(), R.drawable.walter);
        Attachment attachment1 = new Attachment.Builder("walter.jpg", Attachment.TYPE_JPEG).build(walter);
        Buglife.addAttachment(attachment1);

        Buglife.showReporter();
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
