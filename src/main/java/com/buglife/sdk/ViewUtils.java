package com.buglife.sdk;

import android.content.res.Resources;
import android.util.TypedValue;

public class ViewUtils {
    private ViewUtils() {}

    public static float dpToPx(float dp, Resources res) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    public static int navigationBarHeight(Resources res) {
        int id = res.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0) {
            return res.getDimensionPixelSize(id);
        }
        return 0;
    }

    public static int statusBarHeight(Resources res) {
        int id = res.getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return res.getDimensionPixelSize(id);
        }
        return 0;
    }
}
