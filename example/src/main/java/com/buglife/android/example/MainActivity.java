package com.buglife.android.example;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.buglife.sdk.Attachment;
import com.buglife.sdk.Buglife;
import com.buglife.sdk.InvocationMethod;
import com.buglife.sdk.Log;
import com.buglife.sdk.LogDumper;

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Screen recording is only available in Android M & higher
            Button button = (Button) findViewById(R.id.record_screen_button);
            button.setVisibility(View.GONE);
        }

        Log.d("Omg we're logging things");
        Log.e("And I guess this would be an error");
        Log.d("Logging more things from Buglife");
    }

    public void reportBugButtonTapped(View view) {
//        Bitmap screenshot = Buglife.getScreenshotBitmap();
//        Attachment attachment = new Attachment.Builder("Screenshot.png", TYPE_PNG).build(screenshot);
//        Buglife.addAttachment(attachment);
//        Buglife.showReporter();

        StringBuilder stringBuilder = LogDumper.getLog();
        String string = stringBuilder.toString();
        Log.d("Got the logs:\n\n=====\n" + string + "\n=====\n\n\n");
    }

    public void recordScreenButtonTapped(View view) {
        Buglife.startScreenRecording();
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
