package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
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
import java.nio.ByteBuffer;
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

    private static final String MIME_TYPE = "video/avc";
    private static final int DEFAULT_MEDIA_CODEC_FRAME_RATE = 30;
    private static final int VIDEO_SCALE = 25;
    private static final int VIDEO_ENCODING_BITRATE =  1 * 1000 * 1000;
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
    private MediaMuxer mMediaMuxer;
    private Surface mInputSurface;
    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVideoBufferInfo;
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

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        final boolean landscape = mContext.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
        final int scaledDisplayWidth = (displayMetrics.widthPixels * VIDEO_SCALE) / 100;
        final int scaledDisplayHeight = (displayMetrics.heightPixels * VIDEO_SCALE) / 100;
        final int density = displayMetrics.densityDpi;
        final int width, height;
        width = scaledDisplayWidth;
        height = scaledDisplayHeight;
//
        final DateFormat fileFormat = new SimpleDateFormat("'Buglife_'yyyy-MM-dd-HH-mm-ss'.mp4'", Locale.US);
        String outputFilename = fileFormat.format(new Date());
        mOutputFilePath = new File(mOutputDirectory, outputFilename).getAbsolutePath();
        Log.d("output file path = " + mOutputFilePath);

        prepareVideoEncoder(width, height);

        try {
            mMediaMuxer = new MediaMuxer(mOutputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mData);
        Surface surface = mInputSurface;

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME, width, height, density, VIRTUAL_DISPLAY_FLAG_PRESENTATION, surface, null, null);

        // Starts recording
        drainEncoder();

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

    private void prepareVideoEncoder(int videoWidth, int videoHeight) {
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, videoWidth, videoHeight);

        int frameRate = DEFAULT_MEDIA_CODEC_FRAME_RATE;
        int bitRate = VIDEO_ENCODING_BITRATE;

        // Set some required properties. The media codec may fail if these aren't defined.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 0);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

        // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
        } catch (IOException e) {
            releaseEncoders();
        }
    }

    private void releaseEncoders() {
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);

        if (mMediaMuxer != null) {
            if (mMuxerStarted) {
                mMediaMuxer.stop();
            }

            mMediaMuxer.release();
            mMediaMuxer = null;
            mMuxerStarted = false;
        }

        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }

        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        mVideoBufferInfo = null;
        mDrainEncoderRunnable = null;
        mTrackIndex = -1;
    }

    private boolean mMuxerStarted = false;
    private int mTrackIndex = -1;
    private final Handler mDrainHandler = new Handler(Looper.getMainLooper());
    private Runnable mDrainEncoderRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    private static final int DRAIN_INTERVAL = 10;

    private boolean drainEncoder() {
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);

        while (true) {
            int bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);

            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // nothing available yet
                break;
            } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mTrackIndex >= 0) {
                    throw new RuntimeException("format changed twice");
                }
                mTrackIndex = mMediaMuxer.addTrack(mVideoEncoder.getOutputFormat());
                if (!mMuxerStarted && mTrackIndex >= 0) {
                    mMediaMuxer.start();
                    mMuxerStarted = true;
                }
            } else if (bufferIndex < 0) {
                // not sure what's going on, ignore it
            } else {
                ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
                if (encodedData == null) {
                    throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
                }

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mVideoBufferInfo.size = 0;
                }

                if (mVideoBufferInfo.size != 0) {
                    if (mMuxerStarted) {
                        encodedData.position(mVideoBufferInfo.offset);
                        encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                        mMediaMuxer.writeSampleData(mTrackIndex, encodedData, mVideoBufferInfo);
                    } else {
                        // muxer not started
                    }
                }

                mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }

        mDrainHandler.postDelayed(mDrainEncoderRunnable, DRAIN_INTERVAL);
        return false;
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
        } catch (RuntimeException e) {
            Log.e("Error stopping the media recorder", e);
        }

        releaseEncoders();
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
