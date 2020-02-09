package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import androidx.annotation.Nullable;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenProjector {
    private static final String VIRTUAL_DISPLAY_NAME = "buglife";

    private final MediaProjectionManager mMediaProjectionManager;
    private final int mResultCode;
    private final Intent mResultData;

    private int mDensity = -1;
    private @Nullable ScreenFileEncoder mScreenEncoder;
    private @Nullable MediaProjection mMediaProjection;
    private @Nullable VirtualDisplay mVirtualDisplay;

    public ScreenProjector(Builder builder) {
        Context context = builder.mContext;
        mResultCode = builder.mResultCode;
        mResultData = builder.mResultData;

        mMediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mDensity = context.getResources().getDisplayMetrics().densityDpi;
    }

    void setScreenEncoder(ScreenFileEncoder encoder) {
        mScreenEncoder = encoder;
    }

    void start() {
        if (mScreenEncoder == null) {
            throw new RuntimeException("Screen encoder must be set before calling start!");
        }

        mScreenEncoder.start();

        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData); // can return null
        if (mMediaProjection != null) {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                    VIRTUAL_DISPLAY_NAME,
                    mScreenEncoder.getWidth(),
                    mScreenEncoder.getHeight(),
                    mDensity,
                    VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                    mScreenEncoder.getSurface(),
                    null,
                    null
            );
        }
    }

    void stop() {
        if (mScreenEncoder != null) {
            mScreenEncoder.stop();
            mScreenEncoder = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    public static class Builder {
        private final Context mContext;
        private int mResultCode;
        private Intent mResultData;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setResultCode(int resultCode) {
            mResultCode = resultCode;
            return this;
        }

        public Builder setResultData(Intent resultData) {
            mResultData = resultData;
            return this;
        }

        public ScreenProjector build() {
            return new ScreenProjector(this);
        }
    }
}
