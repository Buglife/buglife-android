package com.buglife.android.example;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.buglife.sdk.Attachment;
import com.buglife.sdk.Buglife;
import com.buglife.sdk.FileAttachment;
import com.buglife.sdk.InvocationMethod;

import java.io.File;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.buglife.sdk.Attachment.TYPE_PNG;

public class MainActivity extends AppCompatActivity {
    boolean hasStarted = false;
    LocationManager mLocationManager;
    LocationListener mLocationListener;

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
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!hasStarted) {
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            mLocationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

// Register the listener with the Location Manager to receive location updates
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, 0);
                }
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                hasStarted = true;
                return super.onTouchEvent(event);
            }
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            hasStarted = true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PERMISSION_GRANTED || grantResults[1] == PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            hasStarted = true;
        }
    }

    public void reportBugButtonTapped(View view) {
        FileAttachment screenshot = Buglife.captureScreenshot();
        Buglife.addAttachment(screenshot);
        Buglife.showReporter();
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

    public void parametersButtonTapped(View view) {
        Buglife.showParameters();
    }
}
