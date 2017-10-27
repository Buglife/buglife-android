package com.buglife.sdk;

import android.content.res.Resources;
import android.util.TypedValue;

public class ViewUtils {
    private ViewUtils() {}

    public static float dpToPx(float dp, Resources res) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }
}
