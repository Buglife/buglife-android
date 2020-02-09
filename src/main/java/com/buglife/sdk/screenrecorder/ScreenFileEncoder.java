package com.buglife.sdk.screenrecorder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenFileEncoder {
    private static final String MIME_TYPE = "video/avc";
    private static final int DEFAULT_MEDIA_CODEC_FRAME_RATE = 30;
    private static final int VIDEO_ENCODING_BITRATE =  1 * 1000 * 1000;

    private final File mOutputFile;
    private @Nullable MediaCodec mEncoder;
    private @Nullable MediaMuxer mMuxer;
    private @Nullable Surface mSurface;

    private int mWidth = -1;
    private int mHeight = -1;
    private boolean mMuxerStarted = false;
    private int mTrackIndex = -1;

    public ScreenFileEncoder(Builder builder) {
        mOutputFile = builder.mOutputFile;
        mWidth = builder.mWidth;
        mHeight = builder.mHeight;
    }

    public void start() {
        if (mWidth == -1 || mHeight == -1) {
            throw new RuntimeException("Output size must be set before calling start!");
        }

        setUpEncoder();
        startEncoder();
    }

    public void stop() {
        stopEncoder();
        tearDownEncoder();
    }

    public @NonNull Surface getSurface() {
        if (mSurface == null) {
            throw new RuntimeException("Oops! Looks like you're trying to obtain the Surface without starting the writer!\nStart the writer first before requesting the Surface.");
        }
        return mSurface;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    private void startEncoder() {
        if (mEncoder != null) { // any throw in setUpEncoder can make mEncoder return null
            mEncoder.start();
            mTrackIndex = -1;
        }
    }

    private void stopEncoder() {
        stopMuxer();
        if (mEncoder != null) { // this one's a little pedantic
            mEncoder.stop();
        }
    }

    private void setUpEncoder() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        // Set some required properties. The media codec may fail if these aren't defined.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_ENCODING_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, DEFAULT_MEDIA_CODEC_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, DEFAULT_MEDIA_CODEC_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / DEFAULT_MEDIA_CODEC_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 0);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.setCallback(new MediaCodec.Callback() {
                @Override public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    // Input buffer will be filled via MediaProjection
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    ByteBuffer encodedData = codec.getOutputBuffer(index);
                    if (encodedData == null) {
                        throw new RuntimeException("couldn't fetch buffer at index " + index);
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        info.size = 0;
                    }

                    if (info.size != 0) {
                        if (mMuxerStarted) {
                            encodedData.position(info.offset);
                            encodedData.limit(info.offset + info.size);
                            mMuxer.writeSampleData(mTrackIndex, encodedData, info);
                        }
                    }

                    codec.releaseOutputBuffer(index, false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException error) {
                    error.printStackTrace();
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    // should happen before receiving buffers, and should only happen once
                    if (mTrackIndex >= 0) {
                        throw new RuntimeException("format changed twice");
                    }
                    if (mMuxer != null) {
                        mTrackIndex = mMuxer.addTrack(codec.getOutputFormat());
                        if (mTrackIndex >= 0) {
                            startMuxer();
                        }
                    }
                }
            });

            mSurface = mEncoder.createInputSurface();
            mMuxer = new MediaMuxer(mOutputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tearDownEncoder() {
        if (mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }

        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }

        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
    }

    private void startMuxer() {
        if (mMuxer == null) {
            throw new NullPointerException("Can't start a null MediaMuxer!");
        }

        if (!mMuxerStarted) {
            mMuxer.start();
            mMuxerStarted = true;
        }
    }

    private void stopMuxer() {
        if (mMuxer == null) {
            throw new NullPointerException("Can't stop a null MediaMuxer!");
        }

        if (mMuxerStarted) {
            mMuxer.stop();
            mMuxerStarted = false;
        }
    }

    public static class Builder {
        private int mWidth = -1;
        private int mHeight = -1;
        private File mOutputFile;

        public Builder setWidth(int width) {
            mWidth = width;
            return this;
        }

        public Builder setHeight(int height) {
            mHeight = height;
            return this;
        }

        public Builder setOutputFile(File outputFile) {
            mOutputFile = outputFile;
            return this;
        }

        public ScreenFileEncoder build() {
            return new ScreenFileEncoder(this);
        }
    }
}
