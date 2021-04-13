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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class ParametersActivity extends AppCompatActivity {


    public static final String PREFS_PARAMETERS_NAME = "PrefsParametersFile";
    public static final String PREFS_SHAKE_PARAMETER_NAME = "PrefsShakeParameter";
    public static final String PREFS_SHAKE_THRESHOLD_PARAMETER_NAME = "PrefsShakeThresHoldParameter";
    public static final String PREFS_SHAKE_SLOP_TIME_MS_PARAMETER_NAME = "PrefsShakeSlopTimeMSParameter";
    public static final String PREFS_SHAKE_COUNT_RESET_TIME_MS_PARAMETER_NAME = "PrefsShakeCountResetTimeMSParameter";
    public static final String PREFS_MIN_SHAKE_COUNT_PARAMETER_NAME = "PrefsMinShakeCountParameter";
    private BugContext mBugContext;
    private @NonNull
    ColorPalette mColorPalette;
    private ToggleButton shakeDetectorToggleButton;
    private EditText shakeThresHoldEditText;
    private EditText shakeSloTimeMsEditText;
    private EditText shakeCountResetTimeMsEditText;
    private EditText shakeMinShakeCountEditText;

    public static Intent newStartIntent(Context context) {
        Intent intent = new Intent(context, ParametersActivity.class);
        intent.setFlags(intent.getFlags() | FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public ParametersActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameters);

        mColorPalette = new ColorPalette.Builder(this).build();
        int colorPrimary = mColorPalette.getColorPrimary();
        int titleTextColor = mColorPalette.getTextColorPrimary();
        String titleTextColorHex = ColorPalette.getHexColor(titleTextColor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.parseColor(titleTextColorHex));
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            Drawable drawable = ActivityUtils.getTintedDrawable(this, android.R.drawable.ic_menu_close_clear_cancel, mColorPalette.getTextColorPrimary());

            actionBar.setHomeAsUpIndicator(drawable);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorPrimary));
            actionBar.setTitle(getString(R.string.parameters));
        }

        ActivityUtils.setStatusBarColor(this, mColorPalette.getColorPrimaryDark());

        shakeDetectorToggleButton = (ToggleButton) findViewById(R.id.shakeDetectorToggleButton);
        shakeThresHoldEditText = (EditText) findViewById(R.id.shakeThresHoldEditText);
        shakeSloTimeMsEditText = (EditText) findViewById(R.id.shakeSloTimeMsEditText);
        shakeCountResetTimeMsEditText = (EditText) findViewById(R.id.shakeCountResetTimeMsEditText);
        shakeMinShakeCountEditText = (EditText) findViewById(R.id.shakeMinShakeCountEditText);

        SharedPreferences settings = getSharedPreferences(PREFS_PARAMETERS_NAME, 0);
        boolean shake_params = settings.getBoolean(PREFS_SHAKE_PARAMETER_NAME, true);
        shakeDetectorToggleButton.setChecked(shake_params);
        enableDisableShakeParams(shake_params);

        shakeDetectorToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableDisableShake();
            }
        });

        float shake_threshold_params = settings.getFloat(PREFS_SHAKE_THRESHOLD_PARAMETER_NAME, ShakeDetector.SHAKE_THRESHOLD);
        shakeThresHoldEditText.setText(String.valueOf(shake_threshold_params));

        int shake_slop_time_ms_params = settings.getInt(PREFS_SHAKE_SLOP_TIME_MS_PARAMETER_NAME, ShakeDetector.SHAKE_SLOP_TIME_MS);
        shakeSloTimeMsEditText.setText(String.valueOf(shake_slop_time_ms_params));

        int shake_count_reset_time_ms_params = settings.getInt(PREFS_SHAKE_COUNT_RESET_TIME_MS_PARAMETER_NAME, ShakeDetector.SHAKE_COUNT_RESET_TIME_MS);
        shakeCountResetTimeMsEditText.setText(String.valueOf(shake_count_reset_time_ms_params));

        int shake_count_params = settings.getInt(PREFS_MIN_SHAKE_COUNT_PARAMETER_NAME, ShakeDetector.MIN_SHAKE_COUNT);
        shakeMinShakeCountEditText.setText(String.valueOf(shake_count_params));
    }

    @Override
    protected void onPause() {
        super.onPause();
        recordShakeParams();
    }

    private void recordShakeParams() {
        SharedPreferences settings = getSharedPreferences(PREFS_PARAMETERS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(PREFS_SHAKE_THRESHOLD_PARAMETER_NAME, Float.parseFloat(String.valueOf(shakeThresHoldEditText.getText())));
        editor.putInt(PREFS_SHAKE_SLOP_TIME_MS_PARAMETER_NAME, Integer.parseInt(String.valueOf(shakeSloTimeMsEditText.getText())));
        editor.putInt(PREFS_SHAKE_COUNT_RESET_TIME_MS_PARAMETER_NAME, Integer.parseInt(String.valueOf(shakeCountResetTimeMsEditText.getText())));
        editor.putInt(PREFS_MIN_SHAKE_COUNT_PARAMETER_NAME, Integer.parseInt(String.valueOf(shakeMinShakeCountEditText.getText())));
        editor.commit();
    }

    private void enableDisableShake() {
        SharedPreferences settings = getSharedPreferences(PREFS_PARAMETERS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_SHAKE_PARAMETER_NAME, shakeDetectorToggleButton.isChecked());
        editor.commit();

        enableDisableShakeParams(shakeDetectorToggleButton.isChecked());
    }

    private void enableDisableShakeParams(Boolean shake_params) {
        shakeThresHoldEditText.setEnabled(shake_params);
        shakeSloTimeMsEditText.setEnabled(shake_params);
        shakeCountResetTimeMsEditText.setEnabled(shake_params);
        shakeMinShakeCountEditText.setEnabled(shake_params);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

