package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.buglife.sdk.Buglife;
import com.buglife.sdk.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.graphics.PixelFormat.TRANSLUCENT;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class ScreenRecorder {
    private static final int VIDEO_SCALE = 25;
    private static final int MAX_RECORD_TIME_MS = 30 * 1000;

    private final @NonNull Handler mMainThread = new Handler(Looper.getMainLooper());
    private final @NonNull Context mContext;
    private final File mOutputDirectory;
    private File mOutputFilePath;
    private @Nullable ScreenRecordButton mScreenRecordButton;
    private final @NonNull WindowManager mWindowManager;
    private boolean mIsRecording;
    private CountDownTimer mCountdownTimer;
    private ScreenProjector mScreenProjector;
    private @Nullable Callback mCallback;

    public ScreenRecorder(Context context, int resultCode, Intent data) {
        mContext = context;
        File externalCacheDir = context.getExternalCacheDir();
        mOutputDirectory = new File(externalCacheDir, "Buglife");
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mScreenProjector = new ScreenProjector.Builder(context)
                .setResultCode(resultCode)
                .setResultData(data)
                .build();
    }

    public void start() {
        showOverlay();
        startRecording();
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private void showOverlay() {
        mScreenRecordButton = new ScreenRecordButton(mContext);
        mScreenRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                stopRecording();
            }
        });
        mScreenRecordButton.setCountdownDuration(MAX_RECORD_TIME_MS);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                TRANSLUCENT);

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowManager.addView(mScreenRecordButton, layoutParams);
    }

    private void hideOverlay() {
        if (mScreenRecordButton != null) {
            mScreenRecordButton.hide(new ScreenRecordButton.HideCallback() {
                @Override public void onViewHidden() {
                    mWindowManager.removeView(mScreenRecordButton);
                    mScreenRecordButton = null;
                }
            });
        }
    }

    private void startRecording() {
        Log.d("Starting screen recording");

        if (!mOutputDirectory.exists() && !mOutputDirectory.mkdirs()) {
            Log.e("Unable to create directory for screen recording output: " + mOutputDirectory);
            Toast.makeText(mContext, "Oops! Looks like there was a problem writing video to your device's external storage.", Toast.LENGTH_SHORT).show();
            return;
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        final int scaledDisplayWidth = (displayMetrics.widthPixels * VIDEO_SCALE) / 100;
        final int scaledDisplayHeight = (displayMetrics.heightPixels * VIDEO_SCALE) / 100;

        final DateFormat fileFormat = new SimpleDateFormat("'Buglife_'yyyy-MM-dd-HH-mm-ss'.mp4'", Locale.US);
        fileFormat.setTimeZone(Buglife.getTimeZone());
        String outputFilename = fileFormat.format(new Date());
        mOutputFilePath = new File(mOutputDirectory, outputFilename);
        Log.d("output file path = " + mOutputFilePath);

        // Start projecting
        mScreenProjector.setScreenEncoder(new ScreenFileEncoder.Builder()
                .setWidth(scaledDisplayWidth)
                .setHeight(scaledDisplayHeight)
                .setOutputFile(mOutputFilePath)
                .build());
        mScreenProjector.start();

        mIsRecording = true;
        mCountdownTimer = new CountDownTimer(MAX_RECORD_TIME_MS, 1000) { //create a timer that ticks every second
            public void onTick(long millisecondsUntilFinished) {
                // No op
            }

            public void onFinish() {
                stopRecording();
            }

        }.start();

        Log.d("Screen recording started");
    }

    private void stopRecording() {
        Log.d("Stopping screen recording");

        if (!mIsRecording) {
            throw new Buglife.BuglifeException("Attempted to stop screen recorder, but it isn't currently recording");
        }
        mCountdownTimer.cancel();

        mIsRecording = false;

        hideOverlay();

        mScreenProjector.stop();

        MediaScannerConnection.scanFile(mContext, new String[]{mOutputFilePath.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(final String path, final Uri uri) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Recording complete: " + uri);
                        if (mCallback != null) {
                            mCallback.onFinishedRecording(new File(path));
                        }
                    }
                });
            }
        });
    }

    public interface Callback {
        void onFinishedRecording(File file);
    }
}
