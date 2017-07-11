package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.buglife.sdk.Attachment;
import com.buglife.sdk.Buglife;
import com.buglife.sdk.Log;
import com.buglife.sdk.R;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.media.MediaRecorder.OutputFormat.MPEG_4;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoSource.SURFACE;
import static android.os.Environment.DIRECTORY_MOVIES;
import static com.buglife.sdk.Attachment.TYPE_MP4;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class ScreenRecorder {

    private static final int CAMCORDER_PROFILE_QUALITY = CamcorderProfile.QUALITY_HIGH;
    private static final String MIME_TYPE = "video/mp4";
    private static final int DEFAULT_CAMERA_FRAME_RATE = 30; // Framerate if no camera profile is found
    private static final int VIDEO_SCALE = 100;
    private static final int VIDEO_ENCODING_BITRATE = 8 * 1000 * 1000;
    private static final String VIRTUAL_DISPLAY_NAME = "buglife";
    private static final int MAX_RECORD_TIME_MS = 30 * 1000;

    private final @NonNull Handler mMainThread = new Handler(Looper.getMainLooper());
    private final @NonNull Context mContext;
    private final File mOutputDirectory;
    private String mOutputFilePath;
    private final int mResultCode;
    private final @NonNull Intent mData;
    private @Nullable OverlayView mOverlayView;
    private final @NonNull WindowManager mWindowManager;
    private MediaRecorder mMediaRecorder;
    private MediaProjection mMediaProjection;
    private final MediaProjectionManager mMediaProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private boolean mIsRecording;
    private CountDownTimer mCountdownTimer;

    public ScreenRecorder(Context context, int resultCode, Intent data) {
        mContext = context;
        mResultCode = resultCode;
        mData = data;
        File externalFilesDir = context.getExternalFilesDir(null);
        mOutputDirectory = new File(externalFilesDir, "Buglife");
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mMediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void start() {
        showOverlay();
        startRecording();
    }

    private void showOverlay() {
        mOverlayView = new OverlayView(mContext, new OverlayView.OverlayViewClickListener() {
            @Override
            public void onResize() {
                mWindowManager.updateViewLayout(mOverlayView, mOverlayView.getLayoutParams());
            }

            @Override
            public void onStopButtonClicked() {
                stopRecording();
            }
        });

        mWindowManager.addView(mOverlayView, OverlayView.getLayoutParams(mContext));
    }

    private void hideOverlay() {
        if (mOverlayView != null) {
            mWindowManager.removeView(mOverlayView);
            mOverlayView = null;
        }
    }

    private void startRecording() {
        Log.d("Starting screen recording");

        if (!mOutputDirectory.exists() && !mOutputDirectory.mkdirs()) {
            Log.e("Unable to create directory for screen recording output: " + mOutputDirectory);
            Toast.makeText(mContext, "Oops! Looks like there was a problem writing video to your device's external storage.", Toast.LENGTH_SHORT).show();
            return;
        }

        CamcorderProfile camcorderProfile = CamcorderProfile.get(CAMCORDER_PROFILE_QUALITY);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        final boolean landscape = mContext.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
        final int scaledDisplayWidth = (displayMetrics.widthPixels * VIDEO_SCALE) / 100;
        final int scaledDisplayHeight = (displayMetrics.heightPixels * VIDEO_SCALE) / 100;
        final int density = displayMetrics.densityDpi;
        final int width, height, frameRate;

        if (camcorderProfile != null) {
            int cameraWidth = camcorderProfile.videoFrameWidth;
            int cameraHeight = camcorderProfile.videoFrameHeight;
            frameRate = camcorderProfile.videoFrameRate;

            int rotatedWidth = landscape ? cameraWidth : cameraHeight;
            int rotatedHeight = landscape ? cameraHeight : cameraWidth;

            if (rotatedWidth >= scaledDisplayWidth && rotatedHeight >= scaledDisplayHeight) {
                width = scaledDisplayWidth;
                height = scaledDisplayHeight;
            } else {
                if (landscape) {
                    width = (scaledDisplayWidth * rotatedHeight) / scaledDisplayHeight;
                    height = rotatedHeight;
                } else {
                    width = rotatedWidth;
                    height = (scaledDisplayHeight * rotatedWidth) / scaledDisplayWidth;
                }
            }
        } else {
            width = scaledDisplayWidth;
            height = scaledDisplayHeight;
            frameRate = DEFAULT_CAMERA_FRAME_RATE;
        }

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(SURFACE);
        mMediaRecorder.setOutputFormat(MPEG_4);
        mMediaRecorder.setVideoFrameRate(frameRate);
        mMediaRecorder.setVideoEncoder(H264);
        mMediaRecorder.setVideoSize(width, height);
        mMediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_BITRATE);

        final DateFormat fileFormat = new SimpleDateFormat("'Buglife_'yyyy-MM-dd-HH-mm-ss'.mp4'", Locale.US);
        String outputFilename = fileFormat.format(new Date());
        mOutputFilePath = new File(mOutputDirectory, outputFilename).getAbsolutePath();
        mMediaRecorder.setOutputFile(mOutputFilePath);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e("Error preparing screen recorder", e);
            throw new Buglife.BuglifeException("Error preparing screen recorder");
        }

        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mData);

        Surface surface = mMediaRecorder.getSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME, width, height, density, VIRTUAL_DISPLAY_FLAG_PRESENTATION, surface, null, null);
        mMediaRecorder.start();
        mIsRecording = true;
        mCountdownTimer = new CountDownTimer(MAX_RECORD_TIME_MS, 1000) { //create a timer that ticks every second
            public void onTick(long millisecondsUntilFinished) {
                Button stopButton = mOverlayView.getStopButton();
                String base = mContext.getString(R.string.stop_recording);
                stopButton.setText(base + " " + String.valueOf(millisecondsUntilFinished/1000));
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

        try {
            mMediaProjection.stop();
            mMediaRecorder.stop();
        } catch (RuntimeException e) {
            Log.e("Error stopping the media recorder", e);
        }

        mMediaRecorder.release();
        mVirtualDisplay.release();

        MediaScannerConnection.scanFile(mContext, new String[]{mOutputFilePath}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(final String path, final Uri uri) {
                mMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Recording complete: " + uri);
                        onRecordingFinished(path);
                    }
                });
            }
        });
    }

    private void onRecordingFinished(String path) {
        Attachment attachment;
        File file = new File(path);

        attachment = new Attachment.Builder("ScreenRecording.mp4", TYPE_MP4).build(file);

        Buglife.addAttachment(attachment);
        Buglife.showReporter();
    }
}
