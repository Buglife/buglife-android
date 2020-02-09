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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

final class ColorPalette {
    private static final String BUGLIFE_ATTRIBUTE_PREFIX = "buglife";
    private static final String ATTRIBUTE_COLOR_PRIMARY = "colorPrimary";
    private static final String ATTRIBUTE_COLOR_PRIMARY_DARK = "colorPrimaryDark";
    private static final String ATTRIBUTE_COLOR_ACCENT = "colorAccent";
    private static final String ATTRIBUTE_TEXT_COLOR_PRIMARY = "textColorPrimary";

    private @ColorInt int mColorPrimary;
    private @ColorInt int mColorPrimaryDark;
    private @ColorInt int mColorAccent;
    private @ColorInt int mTextColorPrimary;

    private ColorPalette(@ColorInt int colorPrimary, @ColorInt int colorPrimaryDark, @ColorInt int colorAccent, @ColorInt int textColorPrimary) {
        mColorPrimary = colorPrimary;
        mColorPrimaryDark = colorPrimaryDark;
        mColorAccent = colorAccent;
        mTextColorPrimary = textColorPrimary;
    }

    @ColorInt int getColorPrimary() {
        return mColorPrimary;
    }

    @ColorInt int getColorPrimaryDark() {
        return mColorPrimaryDark;
    }

    @ColorInt int getColorAccent() {
        return mColorAccent;
    }

    @ColorInt int getTextColorPrimary() {
        return mTextColorPrimary;
    }

    static String getHexColor(@ColorInt int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /**
     * Computes the distance between two colors. See http://www.colorwiki.com/wiki/Delta_E:_The_Color_Difference
     */
    private static double getDeltaE(int a, int b) {
        int r1, g1, b1, r2, g2, b2;
        r1 = Color.red(a);
        g1 = Color.green(a);
        b1 = Color.blue(a);
        r2 = Color.red(b);
        g2 = Color.green(b);
        b2 = Color.blue(b);
        int[] lab1 = getLab(r1, g1, b1);
        int[] lab2 = getLab(r2, g2, b2);
        return Math.sqrt(Math.pow(lab2[0] - lab1[0], 2) + Math.pow(lab2[1] - lab1[1], 2) + Math.pow(lab2[2] - lab1[2], 2));
    }

    /**
     * Converts an RGB color to a LAB color
     */
    private static int[] getLab(int R, int G, int B) {
        float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
        float Ls, as, bs;
        float eps = 216.f / 24389.f;
        float k = 24389.f / 27.f;

        final float Xr = 0.964221f;
        final float Yr = 1.0f;
        final float Zr = 0.825211f;

        // Convert RGB to XYZ
        r = R / 255.f; //R 0..1
        g = G / 255.f; //G 0..1
        b = B / 255.f; //B 0..1

        if (r <= 0.04045) {
            r = r / 12;
        } else {
            r = (float) Math.pow((r + 0.055) / 1.055, 2.4);
        }

        if (g <= 0.04045) {
            g = g / 12;
        } else {
            g = (float) Math.pow((g + 0.055) / 1.055, 2.4);
        }

        if (b <= 0.04045) {
            b = b / 12;
        } else {
            b = (float) Math.pow((b + 0.055) / 1.055, 2.4);
        }


        X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        xr = X / Xr;
        yr = Y / Yr;
        zr = Z / Zr;

        if (xr > eps) {
            fx = (float) Math.pow(xr, 1 / 3.);
        } else {
            fx = (float) ((k * xr + 16.) / 116.);
        }

        if (yr > eps) {
            fy = (float) Math.pow(yr, 1 / 3.);
        } else {
            fy = (float) ((k * yr + 16.) / 116.);
        }

        if (zr > eps) {
            fz = (float) Math.pow(zr, 1 / 3.);
        } else {
            fz = (float) ((k * zr + 16.) / 116);
        }

        Ls = (116 * fy) - 16;
        as = 500 * (fx - fy);
        bs = 200 * (fy - fz);

        int[] lab = new int[3];
        lab[0] = (int) (2.55 * Ls + .5);
        lab[1] = (int) (as + .5);
        lab[2] = (int) (bs + .5);
        return lab;
    }

    static class Builder {
        private final Context mAppContext;

        Builder(Context context) {
            mAppContext = context;
        }

        ColorPalette build() {
            int colorPrimary = getBuglifeColorOrColorFromAppTheme(mAppContext, ATTRIBUTE_COLOR_PRIMARY, 0);
            int colorPrimaryDark = getBuglifeColorOrColorFromAppTheme(mAppContext, ATTRIBUTE_COLOR_PRIMARY_DARK, 0);
            int colorAccent = getBuglifeColorOrColorFromAppTheme(mAppContext, ATTRIBUTE_COLOR_ACCENT, colorPrimary);
            int textColorPrimary = getBuglifeColorOrColorFromAppTheme(mAppContext, ATTRIBUTE_TEXT_COLOR_PRIMARY, colorPrimary);
            return new ColorPalette(colorPrimary, colorPrimaryDark, colorAccent, textColorPrimary);
        }

        /**
         * Returns the foreground color if it has enough contrast against the background color.
         * If not, then this will return a new color that has enough contrast.
         */
        private static @ColorInt int getColorOrDistanceColor(int foregroundColor, int backgroundColor) {
            double deltaE = getDeltaE(foregroundColor, backgroundColor);

            if (deltaE < 40.0) {
                int lightAlternative = Color.WHITE;
                int darkAlternative = Color.BLACK;

                if (getDeltaE(lightAlternative, backgroundColor) > getDeltaE(darkAlternative, backgroundColor)) {
                    return lightAlternative;
                } else {
                    return darkAlternative;
                }
            } else {
                return foregroundColor;
            }
        }

        private static @ColorInt int getBuglifeFallbackColor(String colorName) {
            if (colorName.equals(ATTRIBUTE_COLOR_PRIMARY)) {
                return Color.parseColor("#242a33");
            } else if (colorName.equals(ATTRIBUTE_COLOR_PRIMARY_DARK)) {
                return Color.BLACK;
            } else if (colorName.equals(ATTRIBUTE_COLOR_ACCENT)) {
                return Color.parseColor("#00d9c7");
            } else if (colorName.equals(ATTRIBUTE_TEXT_COLOR_PRIMARY)) {
                return Color.WHITE;
            } else {
                return 0;
            }
        }

        private static String buglifeColorName(String colorName) {
            if (colorName.startsWith(BUGLIFE_ATTRIBUTE_PREFIX)) {
                return colorName;
            }

            String capitalizedColorName = colorName.length() == 0 ? colorName : colorName.substring(0, 1).toUpperCase() + colorName.substring(1);
            return BUGLIFE_ATTRIBUTE_PREFIX + capitalizedColorName;
        }

        private static @ColorInt int getBuglifeColorOrColorFromAppTheme(@NonNull Context context, @NonNull String colorName, @ColorInt int backgroundColor) {
            int notFound = 0;

            // First try to get the color using the `buglife` attribute prefix
            int color = getColorFromAppStyleOrColorResources(context, buglifeColorName(colorName), notFound);

            if (color == notFound) {
                // Then try getting the color using its regular color name (i.e. defined by the app's theme)
                color = getColorFromAppStyleOrColorResources(context, colorName, notFound);

                if (color == notFound) {
                    // Then use a fallback color (i.e. Buglife brand colors)
                    color = getBuglifeFallbackColor(colorName);
                }

                // If they didn't set a custom color using the buglife prefix,
                // and the color we fetched from their theme doesn't have enough distance
                // from colorPrimary, then use a color that has more contrast
                if (backgroundColor != 0) {
                    color = getColorOrDistanceColor(color, backgroundColor);
                }
            }

            return color;
        }

        /**
         * Gets a color from the host application's theme.
         *
         * The host application's theme is defined in the app's AndroidManifest.xml;
         * The colorName is defined within the theme itself.
         *
         * @param colorResourceName The name of the color, as defined in the theme (see `styles.xml`)
         * @param fallback If the colorName isn't found, then use this fallback color
         * @return The color for the given colorName
         */
        private static @ColorInt int getColorFromAppStyleOrColorResources(@NonNull Context context, @NonNull String colorResourceName, @ColorInt int fallback) {
            @ColorInt int color = getColorFromAppStyleResources(context, colorResourceName, fallback);

            if (color == fallback) {
                color = getColorFromAppColorResources(context, colorResourceName, fallback);
            }

            return color;
        }

        /**
         * Gets a color from the host app's theme (styles.xml)
         */
        private static @ColorInt int getColorFromAppStyleResources(@NonNull Context context, @NonNull String colorResourceName, @ColorInt int fallback) {
            String packageName = context.getPackageName();
            int themeId = context.getApplicationInfo().theme;
            Resources.Theme theme = context.getTheme();
            Resources resources;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                resources = theme.getResources();
            } else {
                return fallback;
            }

            int colorAttr = resources.getIdentifier(colorResourceName, "attr", packageName);
            int color;
            try {
                TypedArray a = context.getTheme().obtainStyledAttributes(themeId, new int[]{colorAttr});
                color = a.getColor(0, fallback);
                a.recycle();
            } catch (Exception e) { // I count 5 ways this can fail, and we need to guard against them all... the same way.
                color = fallback;
            }

            return color;
        }

        /**
         * Gets a color from the host app's color resources (colors.xml)
         */
        private static @ColorInt int getColorFromAppColorResources(@NonNull Context context, @NonNull String colorResourceName, @ColorInt int fallback) {
            String packageName = context.getPackageName();
            Resources appResources = context.getResources();
            int appColorAttr = appResources.getIdentifier(colorResourceName, "color", packageName);

            if (appColorAttr != 0) {
                return appResources.getColor(appColorAttr);
            } else {
                return fallback;
            }
        }
    }
}
