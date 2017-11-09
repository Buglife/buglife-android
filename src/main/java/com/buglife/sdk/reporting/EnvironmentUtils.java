package com.buglife.sdk.reporting;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.support.annotation.NonNull;

import java.util.Locale;

class EnvironmentUtils {
    private EnvironmentUtils() {/* No instances */}

    static float getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (batteryIntent == null) {
            return 0;
        }

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale);
    }

    @NonNull
    static Locale getLocale(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.locale;
    }

    @NonNull
    static ActivityManager.MemoryInfo getMemoryInfo(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            throw new RuntimeException("Failed to obtain ActivityManager!");
        }
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi;
    }
}
